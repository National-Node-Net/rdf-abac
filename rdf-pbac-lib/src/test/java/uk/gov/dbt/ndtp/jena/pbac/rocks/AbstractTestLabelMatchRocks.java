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

import uk.gov.dbt.ndtp.jena.pbac.PBACTestRunner;
import uk.gov.dbt.ndtp.jena.pbac.labels.Labels;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStoreRocksDB;
import uk.gov.dbt.ndtp.jena.pbac.labels.StoreFmt;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.sse.SSE;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Concrete triple pattern testing (no wildcards) with all Rocks modes
 */
abstract class AbstractTestLabelMatchRocks {

    private static final Node ANY_MARKER = Node.ANY;
    private static final Node s = SSE.parseNode(":s");
    private static final Node s1 = SSE.parseNode(":s1");
    private static final Node p = SSE.parseNode(":p");
    private static final Node p1 = SSE.parseNode(":p1");
    private static final Node o = SSE.parseNode(":o");
    private static final Node o1 = SSE.parseNode(":o1");

    private LabelsStore labels;
    private File dbDirectory;

    protected LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        try {
            dbDirectory = Files.createTempDirectory("tmp" + storeFmt.getClass()).toFile();
            return Labels.createLabelsStoreRocksDB(dbDirectory, labelMode, null, storeFmt);
        } catch (RocksDBException | IOException e) {
            throw new RuntimeException("Unable to create RocksDB label store", e);
        }
    }

    static Stream<Arguments> provideLabelAndStorageFmt() {
        return Stream.of(Arguments.of(null, null));
    }

    void createStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        labels = createLabelsStore(labelMode, storeFmt);

        labels.add(s, p, o, "spo");
        labels.add(s, p, ANY_MARKER, "sp_");
        labels.add(s, ANY_MARKER, ANY_MARKER, List.of("s__", "x__"));
        labels.add(ANY_MARKER, p, ANY_MARKER, "_p_");
        labels.add(ANY_MARKER, ANY_MARKER, ANY_MARKER, List.of("___", "any=true"));
    }

    @AfterEach void destroyStore() {
        dbDirectory.delete();
        dbDirectory = null;
        if(labels instanceof LabelsStoreRocksDB rocksDB) {
            rocksDB.close();
        }
        Labels.rocks.clear();
    }

    static Triple triple(String string) { return SSE.parseTriple(string); }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void label_match_basic(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        LabelsStore emptyLabelStore = createLabelsStore(labelMode, storeFmt);
        Triple t = triple("(:s1 :p1 :o1)");
        List<String> x = emptyLabelStore.labelsForTriples(t);
        assertEquals(List.of(), x);
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void label_match_spo(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        createStore(labelMode, storeFmt);
        match(s, p, o, "spo");
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void label_match_spx(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        createStore(labelMode, storeFmt);
        match(s, p, o1, "sp_");
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void label_match_sxx(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        createStore(labelMode, storeFmt);
        match(s, p1, o1, "s__", "x__");
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void label_match_xpx(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        createStore(labelMode, storeFmt);
        match(s1, p, o1, "_p_");
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void label_match_xxx(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        createStore(labelMode, storeFmt);
        match(s1, p1, o1, "___", "any=true");
        match(s1, p1, o1, "any=true", "___");
    }

    private void match(Node s, Node p, Node o, String...expected) {
        Triple triple = Triple.create(s, p, o);
        List<String> x = labels.labelsForTriples(triple);
        List<String> e = Arrays.asList(expected);
        PBACTestRunner.assertEqualsUnordered(e, x);
    }
}
