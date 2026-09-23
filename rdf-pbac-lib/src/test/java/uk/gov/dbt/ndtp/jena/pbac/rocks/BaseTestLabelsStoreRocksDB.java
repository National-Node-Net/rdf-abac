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

import org.apache.commons.io.FileUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import uk.gov.dbt.ndtp.jena.pbac.PBACTestRunner;
import uk.gov.dbt.ndtp.jena.pbac.labels.Labels;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStoreRocksDB;
import uk.gov.dbt.ndtp.jena.pbac.labels.StoreFmt;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RocksDB store specific tests.
 */
public abstract class BaseTestLabelsStoreRocksDB extends AbstractTestLabelsStoreRocksDB {

    protected File dbDir;

    protected LabelsStore createLabelsStoreRocksDB(final File dbDir, final LabelsStoreRocksDB.LabelMode labelMode, final StoreFmt storeFmt) throws RocksDBException {
        return Labels.createLabelsStoreRocksDB(dbDir, labelMode, null, storeFmt);
    }

    @Override
    protected LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        try {
            dbDir = Files.createTempDirectory("tmpDirPrefix").toFile();
            store = createLabelsStoreRocksDB(dbDir, labelMode, storeFmt);
            return store;
        } catch (IOException | RocksDBException e) {
            throw new RuntimeException("Could not create RocksDB labels store", e);
        }
    }

    protected LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt, Graph graph) {
        throw new RuntimeException("RocksDB labels store does not support graphs");
    }

    protected void deleteLabelsStore() {
        try {
            FileUtils.deleteDirectory(dbDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not delete RocksDB labels store", e);
        }
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labelsStore_closed(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        store = createLabelsStore(labelMode, storeFmt);
        Labels.closeLabelsStoreRocksDB(store);
        assertThrows(RuntimeException.class, () -> {
            List<String> x = store.labelsForTriples(triple1);
        });
    }

    /**
     * It seems more correct to throw an RTE than to return null
     */
    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labelsStore_get_wild(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        store = createLabelsStore(labelMode, storeFmt);
        store.add(triple1, "label1");
        store.add(triple2, "label2");
        assertThrows(RuntimeException.class, () -> {
            List<String> x = store.labelsForTriples(Triple.ANY);  //warning
        });
        assertThrows(RuntimeException.class, () -> {
            List<String> x = store.labelsForTriples(Triple.create(triple1.getSubject(), triple1.getObject(), Node.ANY));  //warning
        });
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labelsStore_distinctLabels_empty(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        store = createLabelsStore(labelMode, storeFmt);
        assertTrue(store.distinctLabels().isEmpty());
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labelsStore_distinctLabels_returnsAllUniqueLabels(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        store = createLabelsStore(labelMode, storeFmt);
        store.add(triple1, "label-1");
        store.add(triple2, "label-2");

        PBACTestRunner.assertEqualsUnordered(List.of("label-1", "label-2"), List.copyOf(store.distinctLabels()));
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labelsStore_distinctLabels_deduplicatesSameLabelOnDifferentTriples(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        store = createLabelsStore(labelMode, storeFmt);
        store.add(triple1, "shared-label");
        store.add(triple2, "shared-label");

        assertEquals(1, store.distinctLabels().size());
        assertTrue(store.distinctLabels().contains("shared-label"));
    }
}
