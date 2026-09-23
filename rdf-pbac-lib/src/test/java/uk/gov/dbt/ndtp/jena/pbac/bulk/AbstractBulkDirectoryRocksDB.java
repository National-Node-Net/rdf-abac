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

package uk.gov.dbt.ndtp.jena.pbac.bulk;

import org.junit.jupiter.api.Assertions;
import uk.gov.dbt.ndtp.jena.pbac.labels.Labels;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStoreRocksDB;
import uk.gov.dbt.ndtp.jena.pbac.labels.StoreFmt;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rocksdb.RocksDBException;

import java.util.stream.Stream;

public abstract class AbstractBulkDirectoryRocksDB extends BulkDirectory {

    @Override
    LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws RocksDBException {
        return Labels.createLabelsStoreRocksDB(dbDir, labelMode, null, storeFmt);
    }

    static Stream<Arguments> provideStorageFmt() {
        return Stream.of();
    }

    @ParameterizedTest
    @MethodSource("provideStorageFmt")
    public void starWarsReadLoad(StoreFmt storeFmt) throws RocksDBException {

        LoadStats stats = bulkLoadAndRepeatedlyRead(CONTENT_DIR, storeFmt,0.01, 1000);
        Assertions.assertNotNull(stats);
        stats.report("starwars files ");
    }

    @Disabled("too big/slow - used for manually checking read capacity")
    @ParameterizedTest
    @MethodSource("provideStorageFmt")
    public void biggerFilesReadLoad(StoreFmt storeFmt) throws RocksDBException {

        var stats = bulkLoadAndRepeatedlyRead(
            directoryProperty("pbac.labelstore.biggerfiles").getAbsolutePath(),
            storeFmt,
            0.001,
            1000);
        Assertions.assertNotNull(stats);
        stats.report("bigger files ");
    }

    @Disabled("too big/slow - used for manually checking read capacity")
    @ParameterizedTest
    @MethodSource("provideStorageFmt")
    public void biggestFilesReadLoad(StoreFmt storeFmt) throws RocksDBException {

        var stats = bulkLoadAndRepeatedlyRead(
            directoryProperty("pbac.labelstore.biggestfiles").getAbsolutePath(),
            storeFmt,
            0.001,
            100); // use 0.0001,1000 to search fewer keys more often, too few keys may cache ?
        Assertions.assertNotNull(stats);
        stats.report("biggest files ");
    }
}
