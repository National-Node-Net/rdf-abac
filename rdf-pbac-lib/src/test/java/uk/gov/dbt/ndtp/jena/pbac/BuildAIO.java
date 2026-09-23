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


package uk.gov.dbt.ndtp.jena.pbac;

import java.util.Objects;

import uk.gov.dbt.ndtp.jena.pbac.assembler.SecuredDatasetAssembler;
import uk.gov.dbt.ndtp.jena.pbac.lib.Attributes;
import uk.gov.dbt.ndtp.jena.pbac.lib.AttributesStore;
import uk.gov.dbt.ndtp.jena.pbac.lib.AuthzException;
import uk.gov.dbt.ndtp.jena.pbac.lib.DatasetGraphPBAC;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStore;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.impl.Util;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.system.G;

/**
 * Ways to build authorization setups from a single file or dataset, of
 * data+labels+attributes graphs.
 * <p>
 * The all-in-one file consists of:
 * <ul>
 * <li>Default graph - the data</li>
 * <li>{@code <http://ndtp.co.uk/security#labels>} - the label graph</li>
 * <li>{@code <test:attributes>} - the attribute store</li>
 * <li>{@code <test:result>} - the expected graph as a projection of the data with security applied.</li>
 * </ul>
 *
 * <p>
 * @see SecuredDatasetAssembler
 */
public class BuildAIO {

    public static DatasetGraphPBAC setupByTriG(String aioFile, String dftTripleAttributes, LabelsStore testSubject) {
//      public static void load(DatasetGraphAuthz dsg, String datafile) {
      Lang lang = RDFLanguages.filenameToLang(aioFile);
      if ( lang == null )
          throw new AuthzException("Can't determine the syntax: "+aioFile);
      if ( !RDFLanguages.isQuads(lang) )
          throw new AuthzException("Not a quads syntax: "+lang);
      DatasetGraph aio = DatasetGraphFactory.createTxnMem();
      RDFParser.source(aioFile).lang(lang).parse(aio);
      return setupByTriG(aio, dftTripleAttributes, testSubject);
    }

    public static DatasetGraphPBAC setupByTriG(DatasetGraph aio, String dftTripleAttributes, LabelsStore testSubject) {
        DatasetGraph data = DatasetGraphFactory.createTxnMem();
        GraphUtil.addInto(data.getDefaultGraph(), aio.getDefaultGraph());
        // use copies to ensure complete isolation in tests.
        return buildFromGraphs(data, testSubject,
                               copy(aio.getGraph(VocabAuthzTestResults.graphForLabels)),
                               copy(aio.getGraph(VocabAuthzTestResults.graphForAttributes)));
    }

    private static Graph copy(Graph graph) {
        Graph safe = GraphFactory.createDefaultGraph();
        graph.getTransactionHandler().execute(()->GraphUtil.addInto(safe, graph));
        return safe;
    }

    /**
     * The labels graphs are copied and not changed by updates.
     * Null for labels is legal and means "none".
     * @param testSubject
     */
    private static DatasetGraphPBAC buildFromGraphs(DatasetGraph data, LabelsStore testSubject, Graph labels, Graph attributesStoreGraph) {
        Objects.requireNonNull(data, "Argument data");
        LabelsStore labelsStore = Objects.requireNonNull(testSubject, "Argument testSubject");

        String datasetDefaultLabel = null;

        if ( labels != null && ! labels.isEmpty() ) {
            labelsStore.addGraph(labels);
            Node x = G.getZeroOrOneSP(labels, null, VocabAuthzTestResults.pDatasetDefaultLabel);
            if ( x != null ) {
                if ( ! Util.isSimpleString(x) )
                    Log.error(BuildAIO.class, "Ignored - not valid for authz:datasetDefaultLabel: "+x);
                else
                    datasetDefaultLabel = x.getLiteralLexicalForm();
            }
        }

        AttributesStore attributesStore = Attributes.buildStore(attributesStoreGraph);
        // No accessAttribute
        DatasetGraphPBAC dsgz = PBAC.authzDataset(data, null, labelsStore, datasetDefaultLabel, attributesStore);
        return dsgz;
    }
}
