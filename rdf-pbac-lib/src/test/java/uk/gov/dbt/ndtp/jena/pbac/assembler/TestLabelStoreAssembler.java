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

package uk.gov.dbt.ndtp.jena.pbac.assembler;

import uk.gov.dbt.ndtp.jena.pbac.labels.Labels;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.pbac.labels.hashing.Hasher;
import uk.gov.dbt.ndtp.jena.pbac.rocks.TestLabelsStoreRocksDBByteBufferConfig;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

import static uk.gov.dbt.ndtp.jena.pbac.lib.VocabAuthzDataset.*;
import static uk.gov.dbt.ndtp.jena.pbac.labels.Labels.closeLabelsStoreRocksDB;
import static uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStoreRocksDB.LabelMode.MERGE;
import static uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStoreRocksDB.LabelMode.OVERWRITE;
import static uk.gov.dbt.ndtp.jena.pbac.labels.TestStoreFmt.assertRocksDBByHash;
import static uk.gov.dbt.ndtp.jena.pbac.labels.TestStoreFmt.assertRocksDBByString;
import static uk.gov.dbt.ndtp.jena.pbac.labels.hashing.HasherUtil.createXX128Hasher;
import static uk.gov.dbt.ndtp.jena.pbac.labels.hashing.HasherUtil.hasherMap;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestLabelStoreAssembler {
    static File dbDirectory;
    Model model;
    LabelsStore store;

    @BeforeEach
    public void setUpFiles() {
        model = ModelFactory.createDefaultModel();
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
        closeLabelsStoreRocksDB(store);
    }

    @Test
    public void test_generateStore_default_ByString_Overwrite() throws RocksDBException {
        // given
        Resource r = model.createResource("test_generateStore_default_ByString_Overwrite");
        // when
        store = LabelStoreAssembler.generateStore(dbDirectory, r);
        // then
        assertNotNull(store);
        assertRocksDBByString(store, OVERWRITE);
    }

    @Test
    public void test_generateStore_ByStringMerge() throws RocksDBException {
        // given
        Resource r = model.createResource("test_generateStore_ByStringMerge");
        r.addLiteral(pLabelsStoreUpdateModeMerge, true);
        r.addLiteral(pLabelsStoreByString, true);
        // when
        store = LabelStoreAssembler.generateStore(dbDirectory, r);
        // then
        assertNotNull(store);
        assertRocksDBByString(store, MERGE);
    }

    @ParameterizedTest
    @MethodSource("provideHasherAndName")
    public void test_generateStore_ByHash_happyPath(String hashName, Hasher expectedHash) throws RocksDBException {
        // given
        Resource r = model.createResource("test_generateStore_ByHash_"+hashName);
        r.addLiteral(pLabelsStoreByHash, true);
        r.addLiteral(pLabelsStoreByHashFunction, hashName);

        // when
        store = LabelStoreAssembler.generateStore(dbDirectory, r);
        // then
        assertNotNull(store);
        assertRocksDBByHash(store, expectedHash);

    }

    @Test
    public void test_generateStore_ByHash_missingAlgorithm_useDefault () throws RocksDBException {
        // given
        Resource r = model.createResource("test_generateStore_ByHash_missingAlgorithm");
        r.addLiteral(pLabelsStoreByHash, true);

        // when
        store = LabelStoreAssembler.generateStore(dbDirectory, r);
        // then
        assertNotNull(store);
        assertRocksDBByHash(store, createXX128Hasher());
    }

    @Test
    public void test_generateStore_ByHash_emptyAlgorithmString_useDefault () throws RocksDBException {
        // given
        Resource r = model.createResource("test_generateStore_ByHash_missingAlgorithm");
        r.addLiteral(pLabelsStoreByHash, true);
        r.addLiteral(pLabelsStoreByHashFunction, "");

        // when
        store = LabelStoreAssembler.generateStore(dbDirectory, r);
        // then
        assertNotNull(store);
        assertRocksDBByHash(store, createXX128Hasher());
    }

    @Test
    public void test_generateStore_ByHash_wrongAlgorithmString_useDefault () throws RocksDBException {
        // given
        Resource r = model.createResource("test_generateStore_ByHash_missingAlgorithm");
        r.addLiteral(pLabelsStoreByHash, true);
        r.addLiteral(pLabelsStoreByHashFunction, "MISSING");

        // when
        store = LabelStoreAssembler.generateStore(dbDirectory, r);
        // then
        assertNotNull(store);
        assertRocksDBByHash(store, createXX128Hasher());
    }

    /**
     * This method provides the hashers and names from the HasherUtils mapping
     */
    protected static Stream<Arguments> provideHasherAndName() {
        // Convert the hasherMap's entries (key-value pairs) to a Stream of Arguments
        return hasherMap.entrySet().stream()
                .map(entry -> Arguments.of(entry.getKey(), entry.getValue().get()));  // Provide both the key and the Hasher
    }

}
