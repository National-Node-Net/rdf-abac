// SPDX-License-Identifier: Apache-2.0
/*
 *  © Crown Copyright 2025. This work has been developed by the National Digital Twin Programme
 *  and is legally attributed to the Department for Business and Trade (UK) as the governing entity.
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

package uk.gov.dbt.ndtp.jena.abac;

import static org.apache.jena.sparql.sse.SSE.parseTriple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import uk.gov.dbt.ndtp.jena.abac.labels.Labels;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.abac.lib.DatasetGraphABAC;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Transactional;
import org.junit.jupiter.api.Test;

public class TestLabelsVocabulary {

    private static final Triple triple1 = parseTriple("(:s :p 123)");
    private static final Triple triple2 = parseTriple("(:s :p 'xyz')");

    private static LabelsStore storeWith(String... labels) {
        LabelsStore store = Labels.createLabelsStoreMem();
        if ( labels.length > 0 )
            store.add(triple1, labels[0]);
        if ( labels.length > 1 )
            store.add(triple2, labels[1]);
        return store;
    }

    @Test public void vocabulary_storedAndDefault() {
        LabelsStore store = storeWith("label1", "label2");
        assertEquals(Set.of("label1", "label2", "dflt"), Labels.vocabulary(store, "dflt"));
    }

    @Test public void vocabulary_noDefault() {
        LabelsStore store = storeWith("label1", "label2");
        assertEquals(Set.of("label1", "label2"), Labels.vocabulary(store, null));
    }

    /** The deny label is a refusal, so must never enter the vocabulary. */
    @Test public void vocabulary_noDefault_denyLabelAbsent() {
        LabelsStore store = storeWith("label1");
        assertEquals(Set.of("label1"), Labels.vocabulary(store, null));
    }

    @Test public void vocabulary_defaultAlsoStored() {
        LabelsStore store = storeWith("dflt", "label2");
        assertEquals(Set.of("dflt", "label2"), Labels.vocabulary(store, "dflt"));
    }

    @Test public void vocabulary_emptyStore_defaultOnly() {
        assertEquals(Set.of("dflt"), Labels.vocabulary(storeWith(), "dflt"));
    }

    @Test public void vocabulary_emptyStore_noDefault() {
        assertEquals(Set.of(), Labels.vocabulary(storeWith(), null));
    }

    /** Patterns loaded from a labels file must be concrete; wildcards are rejected. */
    @Test public void vocabulary_fromLabelsGraph() {
        String labelsGraph = """
            PREFIX authz: <http://ndtp.co.uk/security#>
            PREFIX : <http://example/>
            [ authz:pattern ':s :p1 123' ; authz:label "allowed-u1" ] .
            [ authz:pattern ':s :p1 456' ; authz:label "deny" ] .
            """;
        Graph graph = RDFParser.fromString(labelsGraph, Lang.TTL).toGraph();
        LabelsStore store = Labels.createLabelsStoreMem(graph);
        assertEquals(Set.of("allowed-u1", "deny", "default"), Labels.vocabulary(store, "default"));
    }

    private static DatasetGraphABAC datasetWith(LabelsStore store, String defaultLabel) {
        return ABAC.authzDataset(DatasetGraphFactory.createTxnMem(), store, defaultLabel, null);
    }

    @Test public void labelVocabulary_storedAndDefault() {
        DatasetGraphABAC dsgz = datasetWith(storeWith("label1", "label2"), "dflt");
        assertEquals(Set.of("label1", "label2", "dflt"), dsgz.labelVocabulary());
    }

    @Test public void labelVocabulary_noDefault() {
        DatasetGraphABAC dsgz = datasetWith(storeWith("label1"), null);
        assertEquals(Set.of("label1"), dsgz.labelVocabulary());
    }

    @Test public void labelVocabulary_nonEnumerableStore_throws() {
        DatasetGraphABAC dsgz = datasetWith(new NonEnumerableLabelsStore(), "dflt");
        assertThrows(UnsupportedOperationException.class, dsgz::labelVocabulary);
    }

    @Test public void vocabulary_nonEnumerableStore_throws() {
        LabelsStore store = new NonEnumerableLabelsStore();
        assertThrows(UnsupportedOperationException.class, ()->Labels.vocabulary(store, "dflt"));
    }

    @Test public void vocabulary_nullStore_throws() {
        assertThrows(NullPointerException.class, ()->Labels.vocabulary(null, "dflt"));
    }

    /** Inherits the throwing {@code distinctLabels} default, as {@code LabelsStoreRocksDB} does. */
    private static class NonEnumerableLabelsStore implements LabelsStore {
        @Override public List<String> labelsForTriples(Triple triple) { return List.of(); }
        @Override public Transactional getTransactional() { throw new UnsupportedOperationException(); }
        @Override public void add(Triple triple, List<String> labels) { }
        @Override public void add(Node s, Node p, Node o, List<String> labels) { }
        @Override public void remove(Triple triple) { }
        @Override public boolean isEmpty() { return true; }
        @Override public void forEach(BiConsumer<Triple, List<String>> action) { throw new UnsupportedOperationException(); }
        @Override public Graph asGraph() { return null; }
        @Override public Map<String, String> getProperties() { return Map.of(); }
    }
}
