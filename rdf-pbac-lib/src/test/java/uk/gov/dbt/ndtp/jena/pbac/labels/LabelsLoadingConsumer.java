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

package uk.gov.dbt.ndtp.jena.pbac.labels;

import uk.gov.dbt.ndtp.jena.pbac.SysPBAC;
import uk.gov.dbt.ndtp.platform.play.MessageRequest;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.sparql.core.Quad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Helper functional consumer that knows how to consume messages containing a graph,
 * and add the quads of the graph as (triple, label) to a label store.
 * <p>
 * Used by bulk loading of labelstore tests
 */
public class LabelsLoadingConsumer {

    protected static Logger LOG = LoggerFactory.getLogger(LabelsLoadingConsumer.class);

    static long count;

    public interface LabelHandler {
        public void onAdd(Node subject, Node predicate, Node object, String securityLabel);
    }

    /**
     * Base consumer which generates SPO, S'PO, S''PO... in the label store
     * Shared with some other tests
     *
     * @param messageRequest the message we consume to generate entries in the label store
     * @return Void - so nothing
     */
    public static Void consume(final LabelsStore labelsStore, final MessageRequest messageRequest, final LabelHandler labelHandler) {

        count += 1;
        if (count % 1000 == 0) {
            LOG.debug("bulk load consumed {} files", count);
        }

        var headers = messageRequest.getHeaders();
        assertThat(headers.containsKey(SysPBAC.H_SECURITY_LABEL)).isTrue();
        var securityLabel = headers.get(SysPBAC.H_SECURITY_LABEL);
        var dataSet = RDFParser.create().lang(RDFLanguages.TURTLE).source(messageRequest.getBody()).toDataset();

        labelsStore.getTransactional().execute(() -> {
            for (Iterator<Quad> it = dataSet.asDatasetGraph().find(); it.hasNext(); ) {
                Quad quad = it.next();
                var subject = quad.getSubject();
                var predicate = quad.getPredicate();
                var object = quad.getObject();
                labelsStore.add(subject, predicate, object, securityLabel);
                if (labelHandler != null) {
                    labelHandler.onAdd(subject, predicate, object, securityLabel);
                }
            }
        });

        return null;
    }

    public static Void consume(final LabelsStore labelsStore, final MessageRequest messageRequest) {

        return consume(labelsStore, messageRequest, null);
    }

    public static List<String> labelsForTriple(final LabelsStore labelsStore, final String line) {
        var is = new ByteArrayInputStream(line.getBytes(StandardCharsets.UTF_8));
        var dataSet = RDFParser.create().lang(RDFLanguages.TURTLE).source(is).toDataset();

        Iterator<Quad> it = dataSet.asDatasetGraph().find();
        Quad quad = it.next();
        var subject = quad.getSubject();
        var predicate = quad.getPredicate();
        var object = quad.getObject();
        return labelsStore.labelsForTriples(Triple.create(subject, predicate, object));
    }

    public static void addLabelsForTriple(final LabelsStore labelsStore, final String line, String label) {
        var is = new ByteArrayInputStream(line.getBytes(StandardCharsets.UTF_8));
        var dataSet = RDFParser.create().lang(RDFLanguages.TURTLE).source(is).toDataset();

        Iterator<Quad> it = dataSet.asDatasetGraph().find();
        Quad quad = it.next();
        var subject = quad.getSubject();
        var predicate = quad.getPredicate();
        var object = quad.getObject();
        labelsStore.add(Triple.create(subject, predicate, object), label);
    }

}
