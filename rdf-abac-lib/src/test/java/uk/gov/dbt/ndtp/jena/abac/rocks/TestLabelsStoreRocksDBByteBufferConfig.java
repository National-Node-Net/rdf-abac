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
 *  © Crown Copyright 2026. This work has been developed by the National Digital Twin Programme
 *  and is legally attributed to the UK's Department for Business, Innovation, Science and Trade (BIST) as the governing entity.
 */

package uk.gov.dbt.ndtp.jena.abac.rocks;

import uk.gov.dbt.ndtp.jena.abac.AbstractTestLabelsStore;
import uk.gov.dbt.ndtp.jena.abac.labels.Labels;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStoreRocksDB;
import uk.gov.dbt.ndtp.jena.abac.labels.StoreFmtByString;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.*;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static uk.gov.dbt.ndtp.jena.abac.lib.VocabAuthzDataset.pLabelsStoreByteBufferSize;
import static org.apache.jena.sparql.sse.SSE.parseTriple;
import static org.junit.jupiter.api.Assertions.*;

public class TestLabelsStoreRocksDBByteBufferConfig {
    File dbDirectory;
    static Model model = ModelFactory.createDefaultModel();
    final static Triple HUGE_TRIPLE = parseTriple("(:s :p '" + AbstractTestLabelsStore.HUGE_STRING + "')");

    @BeforeEach
    public void setUpFiles() {
        try {
            dbDirectory = Files.createTempDirectory("tmp" + TestLabelsStoreRocksDBByteBufferConfig.class).toFile();
        } catch (IOException e) {
            throw new RuntimeException("Unable to create RocksDB label store", e);
        }
    }

    @AfterEach
    public void tearDownFiles() {
        Labels.rocks.clear();
        dbDirectory.delete();
    }

    @Test
    public void test_happyConfig_property() {
        Resource r = model.createResource("test_happyConfig_property");
        r.addProperty(pLabelsStoreByteBufferSize, "800000");
        try {
            LabelsStore store = Labels.createLabelsStoreRocksDB(dbDirectory, LabelsStoreRocksDB.LabelMode.OVERWRITE, r, new StoreFmtByString());
            assertNotNull(store);
            store.add(HUGE_TRIPLE, "hugeLabel");
            List<String> x = store.labelsForTriples(HUGE_TRIPLE);
            assertEquals(List.of("hugeLabel"), x);
        } catch (RocksDBException e) {
            throw new RuntimeException("Unable to create RocksDB label store", e);
        }
    }

    @Test
    public void test_badConfig_negative() {
        Resource r = model.createResource("test_badConfig_negative");
        r.addProperty(pLabelsStoreByteBufferSize, "-1");
        assertThrows(RuntimeException.class, () -> Labels.createLabelsStoreRocksDB(dbDirectory, LabelsStoreRocksDB.LabelMode.OVERWRITE, r, new StoreFmtByString()));
    }

    @Test
    public void test_badConfig_string() {
        Resource r = model.createResource("test_badConfig_string");
        r.addProperty(pLabelsStoreByteBufferSize, "Wrong");
        assertThrows(RuntimeException.class, () -> Labels.createLabelsStoreRocksDB(dbDirectory, LabelsStoreRocksDB.LabelMode.OVERWRITE, r, new StoreFmtByString()));
    }

    @Test
    public void test_badConfig_OverMaxInt() {
        Resource r = model.createResource("test_badConfig_OverMaxInt");
        long maxIntValue = Integer.MAX_VALUE;
        r.addLiteral(pLabelsStoreByteBufferSize, ++maxIntValue);
        assertThrows(RuntimeException.class, () -> Labels.createLabelsStoreRocksDB(dbDirectory, LabelsStoreRocksDB.LabelMode.OVERWRITE, r, new StoreFmtByString()));
    }

    @Test
    public void test_happyConfig_long() {
        Resource r = model.createResource("test_happyConfig_long");
        r.addLiteral(pLabelsStoreByteBufferSize, 700000L);
        try {
            LabelsStore store = Labels.createLabelsStoreRocksDB(dbDirectory, LabelsStoreRocksDB.LabelMode.OVERWRITE, r, new StoreFmtByString());
            assertNotNull(store);
            store.add(HUGE_TRIPLE, "hugeLabel");
            List<String> x = store.labelsForTriples(HUGE_TRIPLE);
            assertEquals(List.of("hugeLabel"), x);
        } catch (RocksDBException e) {
            throw new RuntimeException("Unable to create RocksDB label store", e);
        }
    }

    @Test
    public void test_exceptionThrownIfBufferTooSmall() {
        Resource r = model.createResource("test_exceptionThrownIfBufferTooSmall");
        r.addProperty(pLabelsStoreByteBufferSize, "6500");
        try {
            LabelsStore store = Labels.createLabelsStoreRocksDB(dbDirectory, LabelsStoreRocksDB.LabelMode.OVERWRITE, r, new StoreFmtByString());
            assertNotNull(store);
            assertThrows(RuntimeException.class, () -> store.add(HUGE_TRIPLE, "hugeLabel"));
        } catch (RocksDBException e) {
            throw new RuntimeException("Unable to create RocksDB label store", e);
        }
    }

    @Test
    public void test_justLargeEnoughBuffer() {
        Resource r = model.createResource("test_justLargeEnoughBuffer");
        r.addProperty(pLabelsStoreByteBufferSize, "6600");
        try {
            LabelsStore store = Labels.createLabelsStoreRocksDB(dbDirectory, LabelsStoreRocksDB.LabelMode.OVERWRITE, r, new StoreFmtByString());
            assertNotNull(store);
            store.add(HUGE_TRIPLE, "hugeLabel");
            List<String> x = store.labelsForTriples(HUGE_TRIPLE);
            assertEquals(List.of("hugeLabel"), x);
        } catch (RocksDBException e) {
            throw new RuntimeException("Unable to create RocksDB label store", e);
        }
    }
}
