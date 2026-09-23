// SPDX-License-Identifier: Apache-2.0
// Originally developed by Telicent Ltd.; subsequently adapted, enhanced, and maintained by the National Digital Twin Programme.
/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/*
 *  Modifications made by the National Digital Twin Programme (NDTP)
 *  © Crown Copyright 2025. This work has been developed by the National Digital Twin Programme
 *  and is legally attributed to the Department for Business and Trade (UK) as the governing entity.
 */


package uk.gov.dbt.ndtp.jena.pbac.fuseki;

import static java.lang.String.format;

import java.io.InputStream;
import java.util.List;
import java.util.function.BiConsumer;

import uk.gov.dbt.ndtp.jena.pbac.AE;
import uk.gov.dbt.ndtp.jena.pbac.SysPBAC;
import uk.gov.dbt.ndtp.jena.pbac.attributes.AttributeExpr;
import uk.gov.dbt.ndtp.jena.pbac.lib.DatasetGraphPBAC;
import uk.gov.dbt.ndtp.jena.pbac.lib.StreamSplitter;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStore;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.fuseki.servlets.ActionErrorException;
import org.apache.jena.fuseki.servlets.ActionLib;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.ServletOps;
import org.apache.jena.fuseki.system.DataUploader;
import org.apache.jena.fuseki.system.UploadDetails;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.TxnType;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.lang.StreamRDFCounting;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.riot.system.StreamRDFWrapper;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.web.HttpSC;
import org.slf4j.Logger;

/**
 * The process of loading data with labels.
 */
class LabelledDataLoader {

    // Restructure by splitting up the code
    //   Use with FKProcessorSCG

    private record LoaderRequest(String id, Logger log, DatasetGraphPBAC dsgz, InputStream data, String contentTypeStr, String headerLabels) {}

    private record LoaderResponse(long tripleCount, long quadCount, long count, long contentLength, String contentType, Lang lang, String base) {
        public String str() {
            return String.format("Content-Length=%d, Content-Type=%s => %s : Count=%d Triples=%d Quads=%d",
                                 contentLength, contentType,
                                 lang.getName(), count, tripleCount, quadCount);
        }
    }

    /*package*/ static void validate(HttpAction action) {
        DatasetGraph dsg = action.getDataset();
        if ( !(dsg instanceof DatasetGraphPBAC) ) {
            // Should have been caught in validate.
            FmtLog.error(action.log, "[%d] This dataset does not support PBAC security labelling.", action.id);
            ServletOps.error(HttpSC.BAD_REQUEST_400, "This dataset does not support PBAC security labelling.");
            return;
        }
    }

    // Current HTTP/PBAC_DataLoader codepath.
    // Called by PBAC_DataLoader.execute()
    /*package*/ static void execute(HttpAction action) {
        DatasetGraph dsg = action.getDataset();
        DatasetGraphPBAC dsgz = null;
        if ( dsg instanceof DatasetGraphPBAC x )
            dsgz = x;
        if ( dsgz == null ) {
            // Should have been caught in validate.
            FmtLog.error(action.log, "[%d] This dataset does not support PBAC security labelling.", action.id);
            ServletOps.error(HttpSC.BAD_REQUEST_400, "This dataset does not support PBAC security labelling.");
            return;
        }
        action.begin(TxnType.WRITE);
        try {
            // long len = action.getRequestContentLengthLong();

            String hSecurityLabel = action.getRequestHeader(SysPBAC.H_SECURITY_LABEL);
            List<String> headerSecurityLabels = parseAttributeList(hSecurityLabel);
            String dsgDftLabels = dsgz.getDefaultLabel();

            if ( headerSecurityLabels != null )
                FmtLog.info(action.log, "[%d] Security-Label %s", action.id, headerSecurityLabels);
            else
                // Dataset default will apply at use time.
                FmtLog.info(action.log, "[%d] Dataset default label: %s", action.id, dsgDftLabels);

            UploadInfo x = ingestData(action, dsgz, headerSecurityLabels, dsgDftLabels);
            if (x != null) {
                action.log.info(format("[%d] Body: %s", action.id, x.str()));
            }

            action.commit();
            ServletOps.success(action);
            // ServletOps.uploadResponse(action, details);
        } catch (ActionErrorException ex) {
            action.abortSilent();
            throw ex;
        } catch (Throwable ex) {
            action.abortSilent();
            ServletOps.errorOccurred(ex);
            return;
        }
    }

    private static List<String> parseAttributeList(String securityLabelsList) {
        if ( securityLabelsList == null )
            return null;
        List<AttributeExpr> x = AE.parseExprList(securityLabelsList);
        return AE.asStrings(x);
    }

    private static void applyLabels(DatasetGraphPBAC dsgz, Graph labelsGraph) {
        if ( labelsGraph != null && !labelsGraph.isEmpty() )
            dsgz.labelsStore().addGraph(labelsGraph);
    }

    record UploadInfo(long tripleCount, long quadCount, long count, String contentType, long contentLength, Lang lang, String base) {
        public String str() {
            return String.format("Content-Length=%d, Content-Type=%s => %s : Count=%d Triples=%d Quads=%d",
                                 contentLength, contentType, lang,
                                 count, tripleCount, quadCount);
        }
    }

    // ---- Ingestion processing.

    // ==> where?
    /**
     * Ingest labelled data.
     * <p>
     * If it is triples the labels are the ones in the header and can't be in the
     * data body.
     * <p>
     * If it is quads, the data is the default graph and the labels from the header
     * apply but are overridden by the {@code <http://ndtp.co.uk/security#labels>}
     * graph.
     */
    /*package*/ static UploadInfo ingestData(HttpAction action, DatasetGraphPBAC dsgz, List<String> headerLabels, String dsgDftLabel) {
        String base = ActionLib.wholeRequestURL(action.getRequest());
        return ingestData(action, base, dsgz, headerLabels, dsgDftLabel);
    }

    /*package*/ static UploadInfo ingestData(HttpAction action, String base, DatasetGraphPBAC dsgz, List<String> headerLabels, String dsgDftLabel) {
        try {
            // Decide the label to apply when the data does not explicitly set the
            // labels on a triple.
            List<String> labelsForData = headerLabels;

            // Decide default labelling.
            // No storing of labels if the dataset default is enough.
            if ( headerLabels != null && dsgDftLabel != null ) {
                if ( headerLabels.equals(List.of(dsgDftLabel)) ) {
                    // Save space in the label store.
                    // Don't store when the dataset default will apply.
                    // This is advantageous when there is one label for
                    // most of the data, and maybe some exceptions explicit
                    // labelled differently. It relies on the default not
                    // changing though.
                    labelsForData = List.of();
                }
            }

            Lang lang = RDFLanguages.contentTypeToLang(action.getRequestContentType());
            if ( RDFLanguages.isTriples(lang) ) {
                // Triples. We can stream process the data because we know the label
                // to apply ahead of parsing.
                return ingestTriples(action, lang, base, dsgz, labelsForData);
            } else {
                // Quads. (Currently assumed to be the labels graph). This has to be
                // buffered.
                return ingestQuads(action, lang, base, dsgz, labelsForData);
            }
        } catch (RiotException ex) {
            ex.printStackTrace();
        } catch (JenaException ex) {
            ex.printStackTrace();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static UploadInfo ingestTriples(HttpAction action, Lang lang, String base, DatasetGraphPBAC dsgz, List<String> headerLabels) {
        StreamRDF baseDest = StreamRDFLib.dataset(dsgz.getData());
        LabelsStore labelsStore = dsgz.labelsStore();
        BiConsumer<Triple, List<String>> labelledTriplesCollector = (triple, listLabels) -> labelsStore.add(triple, listLabels);
        StreamRDF dest = baseDest;
        if ( headerLabels != null ) {
            // If there are no header labels, nothing to do - send to base
            dest = new StreamLabeller(baseDest, headerLabels, labelledTriplesCollector);
        }

        StreamRDFCounting countingDest = StreamRDFLib.count(dest);
        ActionLib.parse(action, countingDest, lang, base);
        return new UploadInfo(countingDest.countTriples(), countingDest.countQuads(), countingDest.count(),
                              action.getRequestContentType(), action.getRequestContentLengthLong(), lang, base);
    }

    private static class StreamLabeller extends StreamRDFWrapper {

        private final List<String> labels;
        private BiConsumer<Triple, List<String>> labelsHandler;

        StreamLabeller(StreamRDF destination, List<String> labels, BiConsumer<Triple, List<String>> labelsHandler) {
            super(destination);
            this.labels = labels;
            this.labelsHandler = labelsHandler;
        }

        @Override
        public void triple(Triple triple) {
            super.triple(triple);
            if ( labels != null )
                labelsHandler.accept(triple, labels);
        }

        @Override
        public void quad(Quad quad) {
            throw new UnsupportedOperationException("StreamLabeller.quad");
        }
    }

    private static UploadInfo ingestQuads(HttpAction action, Lang lang, String base, DatasetGraphPBAC dsgz, List<String> labelsForData) {
        // We could split the bulk data from the modifications using the fact we are
        // inside a transaction on the dataset. The transaction means we are
        // proceeding optimistically adding to the dataset by streaming to data
        // store, then add labels to the labels store, then commit, making the new
        // triples available in the dataset.
        StreamRDF rdfData = StreamRDFLib.dataset(dsgz.getData());
        // Get all the labels - as they may come before or after the data,
        // we need to collect them together, then process them before the txn commit.
        Graph labelsGraph = GraphFactory.createDefaultGraph();
        StreamRDF stream = new StreamSplitter(rdfData, labelsGraph, labelsForData);

        // Contains: String base = ActionLib.wholeRequestURL(action.getRequest());
        UploadDetails details = DataUploader.incomingData(action, stream);
        applyLabels(dsgz, labelsGraph);
        // UploadDetails is a Fuseki class and has limited accessibility. Convert.
        return new UploadInfo(details.getTripleCount(), details.getQuadCount(), details.getCount(),
                              action.getRequestContentType(), action.getRequestContentLengthLong(), lang, base);
    }
}
