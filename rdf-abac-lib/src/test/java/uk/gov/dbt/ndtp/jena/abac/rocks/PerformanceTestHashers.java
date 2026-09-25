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

import uk.gov.dbt.ndtp.jena.abac.SysABAC;
import uk.gov.dbt.ndtp.jena.abac.labels.Labels;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsLoadingConsumer;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStoreRocksDB;
import uk.gov.dbt.ndtp.jena.abac.labels.StoreFmtByHash;
import uk.gov.dbt.ndtp.jena.abac.labels.hashing.Hasher;
import uk.gov.dbt.ndtp.jena.abac.labels.hashing.HasherUtil;
import uk.gov.dbt.ndtp.platform.play.PlayFiles;
import org.openjdk.jmh.annotations.*;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import static uk.gov.dbt.ndtp.jena.abac.labels.Labels.rocks;
import static uk.gov.dbt.ndtp.jena.abac.labels.LabelsStoreRocksDB.LabelMode.OVERWRITE;

/**
 * JMH Class to run the various hash functions that are available
 */
// Benchmark mode and time unit
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)  // Scope of the state
public class PerformanceTestHashers {

    // JMH Param - the key for the hasher
    @Param({
            "city64",
            "farm64",
            "farmna64",
            "farmuo64",
            "metro64",
            "murmur64",
            "murmur128",
            "sha256",
            "sha512",
            "sip24",
            "wy3",
            "xx32",
            "xx64",
            "xx128"
    })

    private String hasherKey;
    private Hasher hasher;
    private LabelsStore labelsStore;
    private File dbDir;

    @Setup(Level.Trial)
    public void setUp() throws IOException, RocksDBException {
        hasher = HasherUtil.hasherMap.get(hasherKey).get();
        dbDir = Files.createTempDirectory("tmp" + hasherKey).toFile();
        labelsStore = Labels.createLabelsStoreRocksDB(dbDir, OVERWRITE, null, new StoreFmtByHash(hasher));
    }

    @Benchmark
    public void benchmarkHasher() {
        File files = new File("rdf-abac-lib/src/test/files/starwars/content");
        PlayFiles.action(files.getAbsolutePath(),
                message -> LabelsLoadingConsumer.consume(labelsStore, message, null),
                headers -> headers.put(SysABAC.H_SECURITY_LABEL, "security=unknowndefault"));
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (labelsStore instanceof LabelsStoreRocksDB rocksDB) {
            rocksDB.close();
        }
        rocks.clear();
        dbDir.delete();
        dbDir = null;
    }

    // Main method to run the benchmark - takes about 2 hours
    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
