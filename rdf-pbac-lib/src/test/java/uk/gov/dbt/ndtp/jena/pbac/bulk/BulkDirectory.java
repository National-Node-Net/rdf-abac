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

import uk.gov.dbt.ndtp.jena.pbac.SysPBAC;
import uk.gov.dbt.ndtp.platform.play.PlayFiles;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.graph.Triple;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import uk.gov.dbt.ndtp.jena.pbac.labels.Labels;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsException;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsLoadingConsumer;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStoreRocksDB;
import uk.gov.dbt.ndtp.jena.pbac.labels.StoreFmt;

import static uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStoreRocksDB.LabelMode.OVERWRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract base for tests which load large data sets into label stores
 */
public abstract class BulkDirectory {

    protected static Logger LOG = LoggerFactory.getLogger(BulkDirectory.class);

    protected final static String BEFORE_DIR = "src/test/files/starwars/before";
    protected final static String CONTENT_DIR = "src/test/files/starwars/content";
    protected final static String AFTER_DIR = "src/test/files/starwars/after";
    protected final static String DEFAULT_SECURITY_LABEL = "security=unknowndefault";
    protected final static String BACKUP_DIR = "src/test/files/starwars/backup";

    public File dbDir;
    public LabelsStore labelsStore;

    private final static int KNOWN_NUMBER_OF_MESSAGE_LINES = 24368;
    private final static int KNOWN_NUMBER_OF_UNIQUE_TRIPLES = 20831;

    private static String level = null;

    @BeforeAll
    public static void beforeClass() {
        level = LogCtl.getLevel(BulkDirectory.LOG);
        LogCtl.setLevel(BulkDirectory.LOG, "warn");
    }

    @AfterEach
    public void after() {
        if (labelsStore instanceof LabelsStoreRocksDB rocksDB) {
            rocksDB.close();
        }
        Labels.rocks.clear();
    }

    @AfterAll
    public static void afterClass() {
        if (level != null)
            LogCtl.setLevel(BulkDirectory.LOG, "level");
    }

    abstract LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws RocksDBException;

    static Stream<Arguments> provideLabelAndStorageFmt() {
        return Stream.of(Arguments.of(null, null));
    }


    // @Disabled("useful for dumping comprehensive java state")
    // @Test
    private void javaInfo() {
        // Also, add -XX:+PrintFlagsFinal to the run configuration to dump HeapSize etc..
        System.err.println("---Properties---");
        System.getProperties().forEach((k, v) -> System.err.println(k + " --> " + v));
        System.err.println("---Env---");
        System.getenv().forEach((k, v) -> System.err.println(k + " --> " + v));
        System.err.println("---Management---");
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        var arguments = runtimeMxBean.getInputArguments();
        arguments.forEach(System.err::println);
        System.err.println("---Runtime---");
        var rtValues = new HashMap<String, Long>();
        var runtime = Runtime.getRuntime();
        rtValues.put("freeMemory", runtime.freeMemory());
        rtValues.put("totalMemory", runtime.totalMemory());
        rtValues.put("maxMemory", runtime.maxMemory());
        var MB = 1 << 20;
        rtValues.forEach((k, v) -> System.err.println(k + " --> " + v / MB + "MB"));
    }

    protected File directoryProperty(final String property) {

        var externalDir = System.getProperty(property);
        assertThat(externalDir).as("Please define property -D%s=<path-to-directory> in your run configuration", property).isNotEmpty();
        File files = new File(externalDir);
        assertThat(files.isDirectory()).isTrue();

        return files;
    }

    @Disabled("too big/slow - used for manually checking load capacity")
    @ParameterizedTest(name = "{index}: Store = {0}")
    @MethodSource("provideStorageFmt")
    public void biggerFiles(StoreFmt storeFmt) throws RocksDBException {

        labelsStore = createLabelsStore(OVERWRITE, storeFmt);

        File files = directoryProperty("pbac.labelstore.biggerfiles");
        PlayFiles.action(files.getAbsolutePath(),
                message -> LabelsLoadingConsumer.consume(labelsStore, message),
                headers -> headers.put(SysPBAC.H_SECURITY_LABEL, DEFAULT_SECURITY_LABEL));

        final var properties = labelsStore.getProperties();
        LOG.info("properties {}", properties);
    }

    @Disabled("too big/slow - used for manually checking load capacity")
    @ParameterizedTest(name = "{index}: Store = {0}")
    @MethodSource("provideStorageFmt")
    public void biggestFiles(StoreFmt storeFmt) throws RocksDBException {

        labelsStore = createLabelsStore(OVERWRITE, storeFmt);

        File files = directoryProperty("pbac.labelstore.biggestfiles");
        PlayFiles.action(files.getAbsolutePath(),
                message -> LabelsLoadingConsumer.consume(labelsStore, message),
                headers -> headers.put(SysPBAC.H_SECURITY_LABEL, DEFAULT_SECURITY_LABEL));

        final var properties = labelsStore.getProperties();
        LOG.info("properties {}", properties);
    }

    private void playFiles(final String directory) {
        playFiles(directory, null);
    }

    private void playFiles(final String directory, final LabelsLoadingConsumer.LabelHandler labelHandler) {
        File files = new File(directory);
        assertThat(files.isDirectory()).isTrue();
        PlayFiles.action(files.getAbsolutePath(),
                message -> LabelsLoadingConsumer.consume(labelsStore, message, labelHandler),
                headers -> headers.put(SysPBAC.H_SECURITY_LABEL, DEFAULT_SECURITY_LABEL));
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void starWars(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws RocksDBException {

        labelsStore = createLabelsStore(labelMode, storeFmt);

        playFiles(CONTENT_DIR);
        playFiles(AFTER_DIR);

        var labels = LabelsLoadingConsumer.labelsForTriple(labelsStore,
                "<https://starwars.com#grid_R7> <http://ies.data.gov.uk/ontology/ies4#inLocation> <https://starwars.com#AGalaxyFarFarAway> .");

        if (labelsStore instanceof LabelsStoreRocksDB && labelMode == LabelsStoreRocksDB.LabelMode.MERGE) {
            //Check that a member of the AFTER_DIR has BOTH labels
            assertThat(labels.size()).isEqualTo(2);
            assertThat(labels).contains("sensitivity=Ultra");
            assertThat(labels).contains("nationality=GBR");
        } else {
            //Check that a member of the AFTER_DIR has ONLY the latest label (overwrite mode)
            assertThat(labels.size()).isEqualTo(1);
            assertThat(labels).contains("sensitivity=Ultra");
        }

        final var properties = labelsStore.getProperties();
//        LOG.info("properties {}", properties);
        expectedStarWarsProperties().forEach((k, v) -> {
            assertThat(properties.containsKey(k)).isTrue();
            assertThat(properties.get(k)).isEqualTo(v);
        });
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void backupAndRestore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws RocksDBException, IOException {
        // set up
        labelsStore = createLabelsStore(labelMode, storeFmt);
        String tripleString = "<https://starwars.com#grid_R7> <http://ies.data.gov.uk/ontology/ies4#inLocation> <https://starwars.com#AGalaxyFarFarAway> .";
        String labelBefore = "security=unknowndefault";

        if (labelsStore instanceof LabelsStoreRocksDB rocksDB) {
            Path tempDir = Files.createTempDirectory("backup");
            LabelsLoadingConsumer.addLabelsForTriple(labelsStore, tripleString, labelBefore);
            var labels = LabelsLoadingConsumer.labelsForTriple(labelsStore, tripleString);

            assertEquals(1, labels.size());
            assertEquals(labelBefore, labels.get(0));

            // Back-up
            rocksDB.backup(tempDir.toString());

            // Make some changes
            LabelsLoadingConsumer.addLabelsForTriple(labelsStore, tripleString, "security=somethingelse");
            labels = LabelsLoadingConsumer.labelsForTriple(labelsStore, tripleString);

            if (OVERWRITE.equals(labelMode)) {
                assertEquals(1, labels.size());
                assertNotEquals(labelBefore, labels.get(0));
            } else {
                assertNotEquals(1, labels.size());
            }

            // Restore
            rocksDB.restore(tempDir.toString());

            labels = LabelsLoadingConsumer.labelsForTriple(labelsStore, tripleString);
            assertEquals(1, labels.size());
            assertEquals(labelBefore, labels.get(0));

            rocksDB.close();

        }
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void backupAndRestoreFailuresThrowLabelsException(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws RocksDBException, IOException {
        labelsStore = createLabelsStore(labelMode, storeFmt);

        if (labelsStore instanceof LabelsStoreRocksDB rocksDB) {
            Path tempDir = Files.createTempDirectory("backup");

            String missingDir = tempDir.toString() + "/notthere";

            assertThrows(LabelsException.class,
                    () -> rocksDB.backup(missingDir)
            );

            assertThrows(LabelsException.class,
                    () -> rocksDB.restore(missingDir)
            );
        }
    }

    protected static class LoadStats {
        long beginSetup;
        long beginLoad;
        long beginRead;
        long finish;

        void report(final String message) {
            var setup = Duration.of(beginLoad - beginSetup, ChronoUnit.MILLIS);
            var load = Duration.of(beginRead - beginLoad, ChronoUnit.MILLIS);
            var read = Duration.of(finish - beginRead, ChronoUnit.MILLIS);
            LOG.info("{} : setup {}, load {}, read {}", message, setup, load, read);
        }
    }

    protected LoadStats bulkLoadAndRepeatedlyRead(
            final String filesDir,
            final StoreFmt storeFmt,
            final double readFraction,
            final int readRepeat) throws RocksDBException {

        final LoadStats loadStats = new LoadStats();

        Map<Triple, List<String>> known = new HashMap<>();
        var generator = new Random(42L);

        loadStats.beginSetup = System.currentTimeMillis();
        labelsStore = createLabelsStore(OVERWRITE, storeFmt);

        loadStats.beginLoad = System.currentTimeMillis();
        AtomicInteger count = new AtomicInteger();
        AtomicInteger overrideCount = new AtomicInteger();
        playFiles(filesDir, (s, p, o, label) -> {
            count.getAndIncrement();
            var random = generator.nextDouble();
            var triple = Triple.create(s, p, o);
            if (random < readFraction) {
                //change the label, and record the changed label
                overrideCount.getAndIncrement();
                var newLabel = String.format("%s_%d", label, overrideCount.get());
                known.put(triple, List.of(newLabel));
                labelsStore.add(s, p, o, newLabel);
                LOG.debug("Add {}", newLabel);
            } else if (random < readFraction + readFraction) {
                //don't change the label, record the original value to check for
                known.put(triple, List.of(label));
            } else {
                if (known.containsKey(triple)) {
                    //handle a repeat by re-overwriting with the value we hold
                    var newLabel = known.get(triple);
                    labelsStore.add(s, p, o, newLabel);
                }
            }
        });
        LOG.info("{}/{} of database labels overridden for test", known.size(), count);

        var executorService = Executors.newFixedThreadPool(KNOWNLIST_READ_THREADS);

        loadStats.beginRead = System.currentTimeMillis();
        var knownList = new ArrayList<>(known.entrySet());
        for (int i = 0; i < readRepeat; i++) {
            var random = new Random(i);
            Collections.shuffle(knownList, random);
            if (i % 10 == 0) {
                LOG.debug("Shuffled read repeat {} of {}", i, readRepeat);
            }

            executeKnownList(executorService, knownList);
        }

        loadStats.finish = System.currentTimeMillis();

        return loadStats;
    }

    final static int KNOWNLIST_READ_THREADS = 4;

    private void executeKnownList(
            ExecutorService executorService,
            final ArrayList<Map.Entry<Triple, List<String>>> knownList) {
        var futures = new ArrayList<Future<Boolean>>(knownList.size());
        for (var kv : knownList) {
            LOG.debug("Try {} -> {}", kv.getKey(), kv.getValue());
            var future = executorService.submit(() -> {
                var labels = labelsStore.labelsForTriples(kv.getKey());
                assertThat(labels).isEqualTo(kv.getValue());
                return true;
            });
            futures.add(future);
        }
        while (!futures.isEmpty()) {
            try {
                assertThat(futures.remove(0).get()).isEqualTo(true);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Test problem", e);
            }
        }
    }

    protected Map<String, String> expectedStarWarsProperties() {
        return Map.of("size", "20831");
    }

    private static void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                    .map(Path::toFile)
                    .forEach(file -> {
                        if (!file.delete()) {
                            System.err.println("Failed to delete: " + file.getAbsolutePath());
                        }
                    });
            Files.deleteIfExists(path);
        }
    }
}
