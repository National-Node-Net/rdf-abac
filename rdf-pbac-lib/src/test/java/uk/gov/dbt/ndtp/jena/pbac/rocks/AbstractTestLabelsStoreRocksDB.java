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


package uk.gov.dbt.ndtp.jena.pbac.rocks;

import static org.apache.jena.sparql.sse.SSE.parseTriple;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Stream;

import uk.gov.dbt.ndtp.jena.pbac.PBAC;
import uk.gov.dbt.ndtp.jena.pbac.PBACTestRunner;
import uk.gov.dbt.ndtp.jena.pbac.AbstractTestLabelsStore;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.dbt.ndtp.jena.pbac.labels.Labels;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsException;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStoreRocksDB;
import uk.gov.dbt.ndtp.jena.pbac.labels.StoreFmt;

/**
 * Test storing labels, parameterized for LabelsStoreRocksDB.
 * This test suite has tests for both modes of RocksDB (merge and overwrite).
 * <p>
 * See {@link AbstractTestLabelsStore} for the general contract for a labels store.
 * <p>
 * See {@link AbstractTestLabelMatchRocks} for matching labels more generally.s
 */
public abstract class AbstractTestLabelsStoreRocksDB {

    protected static final Triple triple1 = parseTriple("(:s :p 123)");
    protected static final Triple triple2 = parseTriple("(:s :p 'xyz')");

    protected abstract LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt);

    protected abstract LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt, Graph graph);

    protected void deleteLabelsStore(){
    }

    protected void closeLabelsStore(){
        Labels.closeLabelsStoreRocksDB(store);
        Labels.rocks.clear();
        store = null;
    }

    static Stream<Arguments> provideLabelAndStorageFmt() {
        return Stream.of(Arguments.of(null, null));
    }

    protected LabelsStore store;

    @AfterEach
    public void close() {
        deleteLabelsStore();
        closeLabelsStore();
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labelsStore_1(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        store = createLabelsStore(labelMode, storeFmt);
        List<String> x = store.labelsForTriples(triple1);
        assertEquals(List.of(), x);
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labelsStore_2(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        store = createLabelsStore(labelMode, storeFmt);
        store.add(triple1, "triplelabel");
        List<String> x = store.labelsForTriples(triple1);
        assertEquals(List.of("triplelabel"), x);
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labelsStore_3(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        store = createLabelsStore(labelMode, storeFmt);
        store.add(triple1, "label-1");
        store.add(triple2, "label-x");
        store.add(triple1, "label-2");
        List<String> x = store.labelsForTriples(triple1);
        if (this instanceof BaseTestLabelsStoreRocksDB && labelMode == LabelsStoreRocksDB.LabelMode.MERGE) {
            PBACTestRunner.assertEqualsUnordered(List.of("label-1", "label-2"), x);
        } else {
            assertEquals(List.of("label-2"), x);
        }
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labelsStore_4(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        store = createLabelsStore(labelMode, storeFmt);
        store.add(triple1, "label-1");
        store.add(triple2, "label-2");
        List<String> x = store.labelsForTriples(triple1);
        assertEquals(List.of("label-1"), x);
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labels_add_bad_label(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        // Label is a parse error.
        String logLevel = "FATAL";
        store = createLabelsStore(labelMode, storeFmt);
        PBACTestRunner.loggerAtLevel(PBAC.AttrLOG, logLevel, ()-> assertThrows(LabelsException.class, ()->store.add(triple1, "not .. good (LabelsStoreRocksDB)")));
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labels_add_bad_labels_graph(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        store = createLabelsStore(labelMode, storeFmt);
        String gs = """
            PREFIX : <http://example>
            PREFIX authz: <http://ndtp.co.uk/security#>
            [ authz:pattern 'jibberish' ;  authz:label "allowed" ] .
            """;
        Graph addition = RDFParser.fromString(gs, Lang.TTL).toGraph();

        PBACTestRunner.loggerAtLevel(Labels.LOG, "FATAL", ()-> assertThrows(LabelsException.class, ()-> {
                         store.addGraph(addition);
                         store.labelsForTriples(triple1);
        }));
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labels_add_same_triple_different_label(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        store = createLabelsStore(labelMode, storeFmt);
        List<String> x = store.labelsForTriples(triple1);
        assertTrue(x.isEmpty(), "Labels aready exist");
        store.add(triple1, "label-1");
        store.add(triple1, "label-2");
        List<String> labels = store.labelsForTriples(triple1);

        if (this instanceof BaseTestLabelsStoreRocksDB && labelMode == LabelsStoreRocksDB.LabelMode.MERGE) {
            PBACTestRunner.assertEqualsUnordered(List.of("label-1", "label-2"), labels);
        } else {
            assertEquals(List.of("label-2"), labels);
        }
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labels_add_same_triple_same_label(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        store = createLabelsStore(labelMode, storeFmt);
        List<String> x = store.labelsForTriples(triple1);
        store.add(triple1, "TheLabel");
        store.add(triple1, "TheLabel");
        List<String> labels = store.labelsForTriples(triple1);
        assertEquals(List.of("TheLabel"), labels);
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labels_add_triple_duplicate_label_in_list(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        store = createLabelsStore(labelMode, storeFmt);
        List<String> x = store.labelsForTriples(triple1);
        var z = List.of("TheLabel", "TheLabel");
        assertThrows(LabelsException.class, ()->store.add(triple1, z));
    }
}
