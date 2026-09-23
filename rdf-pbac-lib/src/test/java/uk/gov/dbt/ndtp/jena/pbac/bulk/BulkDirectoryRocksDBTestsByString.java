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

import uk.gov.dbt.ndtp.jena.pbac.rocks.LabelAndStorageFormatProviderUtility;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;
import uk.gov.dbt.ndtp.jena.pbac.labels.StoreFmtByString;

/**
 * Run {@link BulkDirectory} tests using the string-based RocksDB label store,
 * using a setup extension which creates the right kind of store.
 */
@ExtendWith(RocksDBSetupExtension.class)
public class BulkDirectoryRocksDBTestsByString extends AbstractBulkDirectoryRocksDB {
    /**
     * This method provides a StorageFmtByString, combined with LabelMode values
     */
    public static Stream<Arguments> provideLabelAndStorageFmt() {
        return LabelAndStorageFormatProviderUtility.provideLabelAndStorageFmtByString();
    }

    /**
     * This method provides a StorageFmtByString
     */
    public static Stream<Arguments> provideStorageFmt() {
        return Stream.of(Arguments.of(new StoreFmtByString()));
    }
}
