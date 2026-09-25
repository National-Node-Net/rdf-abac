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

import uk.gov.dbt.ndtp.jena.abac.labels.*;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStoreRocksDB;
import uk.gov.dbt.ndtp.jena.abac.labels.NaiveNodeTable;
import uk.gov.dbt.ndtp.jena.abac.labels.StoreFmtByHash;
import uk.gov.dbt.ndtp.jena.abac.labels.StoreFmtByNodeId;
import uk.gov.dbt.ndtp.jena.abac.labels.StoreFmtByString;
import uk.gov.dbt.ndtp.jena.abac.labels.TrieNodeTable;
import uk.gov.dbt.ndtp.jena.abac.labels.hashing.Hasher;
import org.junit.jupiter.params.provider.Arguments;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static uk.gov.dbt.ndtp.jena.abac.labels.hashing.HasherUtil.hasherMap;

public class LabelAndStorageFormatProviderUtility {
    private LabelAndStorageFormatProviderUtility() {}
    /**
     * This method provides the relevant StorageFmtByNode with underlying Node Tables, combined with LabelMode values
     */
    public static Stream<Arguments> provideLabelAndStorageFmtByNode() {
        Stream<StoreFmtByNodeId> tableStream = Stream.of(
                new StoreFmtByNodeId(new NaiveNodeTable()),
                new StoreFmtByNodeId(new TrieNodeTable())
        );
        return tableStream.flatMap(nodeTable ->
                Stream.of(LabelsStoreRocksDB.LabelMode.values())   // Stream of LabelMode values
                        .map(labelMode -> Arguments.of(labelMode, nodeTable)) // Combine NodeTable with LabelMode
        );
    }

    /**
     * This method provides a StorageFmtByString, combined with LabelMode values
     */
    public static Stream<Arguments> provideLabelAndStorageFmtByString() {
        Stream<StoreFmtByString> stream = Stream.of(new StoreFmtByString());
        return stream.flatMap(storeFmtByString ->
                Stream.of(LabelsStoreRocksDB.LabelMode.values())   // Stream of LabelMode values
                        .map(labelMode -> Arguments.of(labelMode, storeFmtByString)) // Combine Store by String with LabelMode
        );
    }

    /**
     * This method provides the relevant StorageFmtByHash with underlying Hash, combined with LabelMode values
     */
    public static Stream<Arguments> provideLabelAndStorageFmtByHash() {
        // Get a stream of Hashers from the hasherMap
        Stream<Hasher> hasherStream = hasherMap.values().stream()
                .map(Supplier::get);  // Get each Hasher from the Supplier

        // Combine each LabelMode with each Hasher and create a StorageFmtHash
        return hasherStream.flatMap(hasher ->
                Stream.of(LabelsStoreRocksDB.LabelMode.values())   // Stream of LabelMode values
                        .map(labelMode -> {
                            // Create a new instance of StorageFmtHash with the hasher
                            StoreFmtByHash storageFmtHash = new StoreFmtByHash(hasher);
                            // Return both the LabelMode and the StorageFmtHash as arguments
                            return Arguments.of(labelMode, storageFmtHash);
                        })
        );

    }
}
