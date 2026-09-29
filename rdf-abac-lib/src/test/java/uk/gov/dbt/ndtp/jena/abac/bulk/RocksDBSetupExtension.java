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

package uk.gov.dbt.ndtp.jena.abac.bulk;

import uk.gov.dbt.ndtp.jena.abac.labels.Labels;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStore;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Common base setup extension for RocksDB-based label store tests.
 * <p>
 * Concrete subclasses know how to create different kinds of label stores for testing.
 */
class RocksDBSetupExtension implements BeforeEachCallback, AfterEachCallback {

    private File dbDir;
    private LabelsStore labelsStore;

    private final ExecutorService shellExecutorService = Executors.newSingleThreadExecutor();

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        BulkDirectory.LOG.info("RocksDB content before compaction");
        logRocksDBContents(dbDir);
        Labels.compactLabelsStoreRocksDB(labelsStore);
        BulkDirectory.LOG.info("RocksDB content after compaction");
        logRocksDBContents(dbDir);
        Labels.closeLabelsStoreRocksDB(labelsStore);
        FileUtils.deleteDirectory(dbDir);
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        dbDir = Files.createTempDirectory("tmpDirPrefix").toFile();
        BulkDirectory.LOG.info("RocksDB directory {} for test {}", dbDir, extensionContext.getDisplayName());

        //Set the "dbDir" field in the test itself to be the one we just created
        var rocksDBTests = extensionContext.getTestInstance().get();
        var clz = rocksDBTests.getClass();
        clz.getField("dbDir").set(rocksDBTests, dbDir);
    }

    private void logRocksDBContents(final File dbDir) {
        try {
            AtomicLong MB = new AtomicLong();
            AtomicLong sstFileCount = new AtomicLong();
            Consumer<String> lineFn = (String s) -> {
                if (BulkDirectory.LOG.isDebugEnabled()) {
                    System.out.println(s);
                }
                var columns = s.split( "\\s+");
                if (columns.length > 8 &&
                    columns[8].endsWith(".sst")) {
                    MB.addAndGet(Long.parseLong(columns[4]) >> 20);
                    sstFileCount.incrementAndGet();
                }
            };
            var process = Runtime.getRuntime().exec(new String[]{
                "ls", "-l", dbDir.toString()
            });
            StreamGobbler streamGobbler =
                new StreamGobbler(process.getInputStream(), lineFn);
            Future<?> future = shellExecutorService.submit(streamGobbler);

            int exitCode = process.waitFor();
            future.get(5, TimeUnit.SECONDS);
            BulkDirectory.LOG.info("SST file count " + sstFileCount + ", total MB " + MB);
        } catch (IOException | ExecutionException | InterruptedException | TimeoutException e) {
            BulkDirectory.LOG.error("Could not dump RocksDB info", e);
        }
    }

    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                .forEach(consumer);
        }
    }
}

