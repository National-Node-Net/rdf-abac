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


package uk.gov.dbt.ndtp.jena.abac.labels;

import static org.apache.jena.sparql.util.NodeUtils.nullToAny;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;

import uk.gov.dbt.ndtp.jena.abac.AE;
import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeExpr;
import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.atlas.lib.CacheFactory;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.tdb2.sys.NormalizeTermsTDB2;
import org.rocksdb.*;
import uk.gov.dbt.ndtp.jena.abac.lib.VocabAuthzDataset;

/**
 * A labels store implemented using the RocksDB (key,value)-store.
 * <p>
 * An instance may store label nodes by string value, or by id. The difference is
 * encoded by the {@link StoreFmt} supplied at the constructor, which is responsible
 * for taking singleton nodes and node-triples and formatting them as a byte-sequence
 * to supply to RocksDB as the key part of the (key,value)-pair.
 * <p>
 * If label nodes are stored by id, the {@link StoreFmt} will contain a
 * {@link org.apache.jena.tdb2.store.nodetable.NodeTable} with which to perform the
 * mapping, though that is invisible to the {@code LabelsStoreRocksDB}.
 */
public class LabelsStoreRocksDB implements LabelsStore, AutoCloseable {

    /**
     * Control looking for labels ({@link #labelsForSPO}).
     * <p>
     * This store supports patterns but the functionality is not in use (July 2024)
     * If in uses, the first pattern to match in order is the result. Patterns are:
     * <ul>
     * <li>SPO(concrete triple)</li>
     * <li>S__ (label by subject)</li>
     * <li>_P_ (label by predicate)</li>
     * <li>___ (any)
     * </ul>
     * See also {@link LabelsStoreMemPattern}
     * <p>
     * This switch controls whether to look in the pattern column families if there
     * is no direct SPO match. It is set if a pattern is added (see {@link #addRule(Node, Node, Node, List)}).
     * <p>
     * If being used only for defined concrete triple to label, then either the SPO
     * lookup succeeds or there is no label for the triple, but it still makes calls
     * into RocksDB which can be a significant cost.
     * <p>
     * {@code patternsLoaded} is set false initially (faster lookups) and changes to
     * true if a pattern is loaded. No attempt is made to switch back to non-pattern
     * lookups as labels get deleted. That would require counting and testing for
     * duplicate additions.
     * <p>
     * Pattern labels are not current in use. They are in the test suite.
     * <p>
     * See {@link #labelsForSPO}.
     */
    private boolean patternsLoaded = false;
    final static AtomicLong keyTotalSize = new AtomicLong();
    final static AtomicLong valueTotalSize = new AtomicLong();

    final static int DEFAULT_BUFFER_CAPACITY = 1048576;

    public enum LabelMode {
        OVERWRITE {
            @Override
            public void writeUsingMode(TransactionalRocksDB transactionalRocksDB, ColumnFamilyHandle columnFamilyHandle, ByteBuffer key,
                                       ByteBuffer value) {
                if ( Labels.LOG.isDebugEnabled() ) {
                    keyTotalSize.addAndGet(key.limit());
                    valueTotalSize.addAndGet(value.limit());
                }
                transactionalRocksDB.put(columnFamilyHandle, key, value);
            }
        },
        MERGE {
            @Override
            public void writeUsingMode(TransactionalRocksDB transactionalRocksDB, ColumnFamilyHandle columnFamilyHandle, ByteBuffer key,
                                       ByteBuffer value) {
                if ( Labels.LOG.isDebugEnabled() ) {
                    keyTotalSize.addAndGet(key.limit());
                    valueTotalSize.addAndGet(value.limit());
                }
                transactionalRocksDB.merge(columnFamilyHandle, key, value);
            }
        };

        public abstract void writeUsingMode(TransactionalRocksDB transactionalRocksDB, ColumnFamilyHandle columnFamilyHandle,
                                            ByteBuffer key, ByteBuffer value);
    };

    final LabelMode labelMode;

    /**
     * Cache of triple lookup in {@link #labelsForSPO}. This is both a hit-and-miss
     * cache because a miss is a result of List.of().
     * The cache is maintained by {@link #add(Triple, List)} and {@link #add(Node, Node, Node, List)}.
     */
    private static int LABEL_LOOKUP_CACHE_SIZE = 1_000_000;
    // Hit cache of triple to list of strings (labels).
    private Cache<Triple, List<String>> tripleLabelCache = CacheFactory.createCache(LABEL_LOOKUP_CACHE_SIZE);

    /**
     * We maintain a buffer per-thread for key encoding and label encoding to avoid
     * continual re-allocation.
     */
    protected final ThreadLocal<ByteBuffer> keyBuffer;
    protected final ThreadLocal<ByteBuffer> labelsBuffer;

    protected final StoreFmt.Encoder encoder;
    protected final StoreFmt.Parser parser;

    protected final ThreadLocal<ReadOptions> readOptions = ThreadLocal.withInitial(ReadOptions::new);

    protected final ThreadLocal<ByteBuffer> valueBuffer;

    private final AtomicBoolean openFlag = new AtomicBoolean(false);

    private final int bufferCapacity;

    private final String dbPath;

    /**
     * A Function to normalize RDF literal terms.
     * Normalization means to use the node form (for literals) that round-trips with TDb2 storing values.
     * Normalization is appled on storage ({@link #addRule(Node, Node, Node, List)})
     * and lookup ({@link #labelsForTriples(Triple)}).
     * <p>
     * A literal like "10"^^xsd:double has a round-trip form
     * "10e0"^^xsd:double.
     * <p>
     * A literal like "0.123456789"^^xsd:float has a
     * round-tripform 0.12345679"^^xsd:float due to the precision of float values.
     * Precision also affects xsd:double.
     */
    private static Function<Node,Node> normalizeFunction = NormalizeTermsTDB2::normalizeTDB2;

    protected TransactionalRocksDB txRocksDB;
    private RocksDB db;

    /**
     * A RocksDB column family for each of the layers of the label lookup algorithm.
     */
    protected ColumnFamilyHandle cfhSPO;

    protected ColumnFamilyHandle cfhS__;

    protected ColumnFamilyHandle cfh_P_;

    protected ColumnFamilyHandle cfh___;

    private List<ColumnFamilyHandle> allColumnFamilies() {
        return List.of(cfhSPO, cfhS__, cfh_P_, cfh___);
    }

    private final static String CREATE_MESSAGE = "create of RocksDB label store";

    /**
     * The single key used in the wildcard column family is the byte 0xa
     */
    protected final static ByteBuffer KEY_cfh___ = ByteBuffer.allocateDirect(1).put((byte)0xa).flip();

    private ByteBuffer allocateKVBuffer() {
        return ByteBuffer.allocateDirect(bufferCapacity).order(ByteOrder.LITTLE_ENDIAN);
    }

    // Cached parsing on attribute expressions.
    // Use a small cache to cover the common case of all the labels being the same.
    private final static Cache<String, AttributeExpr> cache = CacheFactory.createOneSlotCache();
    /** Parse an attribute expressions - a label */
    private static AttributeExpr parseAttrExpr(String str) {
        return cache.get(str, AE::parseExpr);
    }

    /**
     * Obtain the byte buffer capacity value from configuration if available.
     * @param resource RDF Node representing the configuration
     */
    private static int getByteBufferSize(Resource resource) {
        if (resource != null && resource.hasProperty(VocabAuthzDataset.pLabelsStoreByteBufferSize)) {
            Statement statement = resource.getProperty(VocabAuthzDataset.pLabelsStoreByteBufferSize);
            try {
                int capacity = statement.getInt();
                if (capacity > 0) {
                    return capacity;
                } else {
                    throw new RuntimeException("The RocksDB buffer capacity is invalid value.");
                }
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("The RocksDB buffer capacity is in wrong format.");
            }
        }
        return DEFAULT_BUFFER_CAPACITY;
    }

    /**
     * Set up performance tuning options for column family options as recommended by
     * <a href=
     * "https://github.com/facebook/rocksdb/wiki/Setup-Options-and-Basic-Tuning">Setup-Options-and-Basic-Tuning</a>
     *
     * @return column family options configured as recommended
     */
    private ColumnFamilyOptions configureRocksDBColumnFamilyOptions() {
        var options = new ColumnFamilyOptions();
        options.setLevelCompactionDynamicLevelBytes(true);
        options.setCompressionType(CompressionType.LZ4_COMPRESSION);
        options.setBottommostCompressionType(CompressionType.ZSTD_COMPRESSION);

        return options;
    }

    /**
     * Set up performance tuning options for database options as recommended by
     * <a href=
     * "https://github.com/facebook/rocksdb/wiki/Setup-Options-and-Basic-Tuning">Setup-Options-and-Basic-Tuning</a>
     *
     * @return database options configured as recommended
     */
    private DBOptions configureRocksDBOptions() {
        var options = new Options();

        Labels.LOG.debug("Configure RocksDB options from defaults to recommended:");
        Labels.LOG.debug("maxBackgroundJobs {} to {}", options.maxBackgroundJobs(), 6);
        options.setMaxBackgroundJobs(6);
        Labels.LOG.debug("bytesPerSync {} to {}", options.bytesPerSync(), 1048576);
        options.setBytesPerSync(1048576);
        Labels.LOG.debug("compactionPriority {} to {}", options.compactionPriority(), CompactionPriority.MinOverlappingRatio);
        options.setCompactionPriority(CompactionPriority.MinOverlappingRatio);

        var tableOptions = new BlockBasedTableConfig();
        Labels.LOG.debug("blockSize {} to {}", tableOptions.blockSize(), (long)16 * 1024);
        tableOptions.setBlockSize((long)16 * 1024);
        Labels.LOG.debug("cacheIndexAndFilterBlocks {} to {}", tableOptions.cacheIndexAndFilterBlocks(), true);
        tableOptions.setCacheIndexAndFilterBlocks(true);
        Labels.LOG.debug("pinL0FilterAndIndexBlocksInCache {} to {}", tableOptions.pinL0FilterAndIndexBlocksInCache(), true);
        tableOptions.setPinL0FilterAndIndexBlocksInCache(true);
        var newFilterPolicy = new BloomFilter(10.0);
        Labels.LOG.debug("filterPolicy {} to {}", tableOptions.filterPolicy(), newFilterPolicy);
        tableOptions.setFilterPolicy(newFilterPolicy);
        Labels.LOG.debug("formatVersion {} to {}", tableOptions.formatVersion(), 5);
        tableOptions.setFormatVersion(5);

        options.setTableFormatConfig(tableOptions);

        return new DBOptions(options);
    }

    /**
     * Create a RocksDB-based label store
     *
     * @param dbRoot file into which to save the database
     * @param storeFmt formatter to transform node(s) into byte arrays.
     * @param labelMode whether to overwrite or merge when updating entries.
     */
    /* package */ LabelsStoreRocksDB(final File dbRoot, final StoreFmt storeFmt, final LabelMode labelMode, Resource resource) {
        this.bufferCapacity = getByteBufferSize(resource);
        this.keyBuffer = ThreadLocal.withInitial(this::allocateKVBuffer);
        this.valueBuffer = ThreadLocal.withInitial(this::allocateKVBuffer);
        this.labelsBuffer = ThreadLocal.withInitial(this::allocateKVBuffer);

        this.encoder = storeFmt.createEncoder();
        this.parser = storeFmt.createParser();

        this.labelMode = labelMode;

        this.dbPath = dbRoot.getAbsolutePath();
        openDB();
    }

    private void openDB() {
        final ColumnFamilyOptions columnFamilyOptions = configureRocksDBColumnFamilyOptions().setMergeOperator(new StringAppendOperator(""));

        final var defaultDescriptor = new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, columnFamilyOptions);
        final var cfhSPODescriptor = new ColumnFamilyDescriptor("CF_ABAC_SPO".getBytes(), columnFamilyOptions);
        // TODO (AP) we need a native comparator if we are to have any kind of
        // comparator. The performance of Java-based comparators is far too poor for our use case
        // a comparator is necessary to ensure S,P,O and S,P,Any are close in lookup
        // which may improve performance but is probably not vital...
        final var cfhS__Descriptor = new ColumnFamilyDescriptor("CF_ABAC_S**".getBytes(), columnFamilyOptions);
        final var cfh_P_Descriptor = new ColumnFamilyDescriptor("CF_ABAC_*P*".getBytes(), columnFamilyOptions);
        final var cfh___Descriptor = new ColumnFamilyDescriptor("CF_ABAC_***".getBytes(), columnFamilyOptions);

        final List<ColumnFamilyHandle> columnFamilyHandleList = new ArrayList<>();

        if ( !openFlag.compareAndSet(false, true) ) {
            throw new RuntimeException("Race condition during " + CREATE_MESSAGE);
        }

        try (final DBOptions dbOptions = configureRocksDBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true)) {
            List<ColumnFamilyDescriptor> columnFamilyDescriptorList = List.of(defaultDescriptor, cfhSPODescriptor, cfhS__Descriptor,
                    cfh_P_Descriptor, cfh___Descriptor);
            db = RocksDB.open(dbOptions, dbPath, columnFamilyDescriptorList, columnFamilyHandleList);
            txRocksDB = new TransactionalRocksDB(db);
        } catch (RocksDBException e) {
            if ( !openFlag.compareAndSet(true, false) ) {
                throw new RuntimeException("Race condition during failing " + CREATE_MESSAGE);
            }
            Labels.LOG.error("Unable to open/create RocksDB label store: {}", dbPath, e);
            throw new RuntimeException("Failed " + CREATE_MESSAGE, e);
        }

        // Ignore the default CFH which we never use.
        columnFamilyHandleList.remove(0);

        cfhSPO = columnFamilyHandleList.remove(0);
        cfhS__ = columnFamilyHandleList.remove(0);
        cfh_P_ = columnFamilyHandleList.remove(0);
        cfh___ = columnFamilyHandleList.remove(0);
    }

    @Override
    public List<String> labelsForTriples(Triple triple) {
        triple = tripleNormalize(triple);
        return tripleLabelCache.get(triple, t->labelsForTriples(t.getSubject(), t.getPredicate(), t.getObject()));
    }

    // Convert a triple so that nulls become ANY and object literals are normalized.
    // Returns the input object if there is no change.
    private static Triple tripleNormalize(Triple triple) {
        Node s = nullToAny(triple.getSubject());
        Node p = nullToAny(triple.getPredicate());
        Node o = nullToAny(triple.getObject());
        if ( normalizeFunction != null )
            o = normalizeFunction.apply(o);
        if ( s == triple.getSubject() && p == triple.getPredicate() && o == triple.getObject() )
            return triple;
        return Triple.create(s, p, o);
    }



    /**
     * Perform a lookup in the labels store, given the SPO-triple (encoded as 3
     * separate nodes), return the (list of) labels which are the answer to "what is
     * the most specific set of labels for this triple ?" - first, if a value (list
     * of labels) is held for SPO is held, return that - second, if a value (list of
     * labels) is held for SP_ (a wildcard on object) is held, return that - third,
     * if a value (list of labels) is held for S__ (a wildcard for predicate and
     * object) - fourth, _P_ (a wildcard for subject and object) - fifth, a complete
     * wildcard/backstop list of values.
     *
     * @param subject part of the triple
     * @param predicate part of the triple
     * @param object part of the triple
     * @return a list/set of labels
     */
    private List<String> labelsForTriples(final Node subject, final Node predicate, final Node object) {
        var pattern = ABACPattern.fromTriple(subject, predicate, object);
        if ( pattern != ABACPattern.PATTERN_SPO) {
            var msg = "Asked for labels for a triple with wildcards: " + NodeFmtLib.displayStr(Triple.create(subject, predicate, object));
            throw new IllegalArgumentException(msg);
        }

        try {
            List<String> result = labelsForSPO(subject, predicate, object);
            return result;
        } catch (RocksDBException e) {
            throw new RuntimeException("Label store failed on lookup " + NodeFmtLib.displayStr(Triple.create(subject, predicate, object)), e);
        }
    }

    @Override
    public void forEach(BiConsumer<Triple, List<String>> action) {
        throw new NotImplemented(this.getClass().getSimpleName()+".forEach");
    }

    /**
     * Clear the triple lookup cache
     */
    public void clearTripleLookupCache() {
        tripleLabelCache.clear();
    }

    /**
     * Get the labels held for a particular key.
     * <p>
     * There may be multiple entries of the form (count,lengths[],strings[]) as
     * multiple add()s will result in RocksDB merging the values it has received, by
     * concatenation.
     *
     * @param valueBuffer holding the labels
     * @param labels list which is to receive the final set of labels
     * @return the list of labels, which contains a set of labels
     */
    private List<String> getLabels(final ByteBuffer valueBuffer, final List<String> labels) {
        var set = new HashSet<String>();
        while (valueBuffer.position() < valueBuffer.limit()) {
            parser.parseStrings(valueBuffer, set);
        }
        labels.addAll(set);

        return labels;
    }

    /**
     * Perform the core of lookup by accessing each of the RocksDB column families in
     * priority order
     * <p>
     * Is there a match for S,P,O or S,P,_ ? The SPO column family also stores values
     * for S,P,Any. These are fetched as a single {@code multiGet()}, but it may be
     * more efficient, depending on workload, to do 2 single gets, if the first
     * (S,P,O) usually succeeds this will avoid the overhead of a {@code multiGet()}
     * The choice depends on what is the common case for whether a particular S,P,O
     * key will have its own label value stored, or whether S,P,Any is more likely to
     * be the first hit on lookup.
     * <p>
     * Using {@code multiGet()} avoids the need to perform scans of iterators, which
     * in turn would require a custom comparator so that Rocks stores keys in natural
     * S,P,O < S,P,* order.. If changing the implementation to do this is
     * contemplated, any comparator MUST be written as a C++ comparator (not Java) in
     * order not to destroy performance.
     * </p>
     *
     * @param subject part of the triple
     * @param predicate part of the triple
     * @param object part of the triple
     * @return a list/set of labels
     * @throws RocksDBException if something went wrong with the database lookup
     */
    private List<String> labelsForSPO(final Node subject, final Node predicate, final Node object) throws RocksDBException {
        if (db == null) {
            throw new RuntimeException("The RocksDB labels store appears to be closed.");
        }

        ByteBuffer key = keyBuffer.get().clear();
        ByteBuffer valueBuffer = this.valueBuffer.get().clear();

        encoder.formatTriple(key, subject, predicate, object);
        ReadOptions readOptionsInstance = readOptions.get();

        var labels = new ArrayList<String>();
        // Checking S,P,O
        if (db.get(cfhSPO, readOptionsInstance, key.flip(), valueBuffer) != RocksDB.NOT_FOUND) {
            return getLabels(valueBuffer, labels);
        }

        // No pattern support
        if ( ! patternsLoaded )
            return List.of();

        key.clear();
        encoder.formatTriple(key, subject, predicate, Node.ANY);

        // Checking S,P,_
        if (db.get(cfhSPO, readOptionsInstance, key.flip(), valueBuffer) != RocksDB.NOT_FOUND) {
            return getLabels(valueBuffer, labels);
        }

        // Checking S,_,_
        // Is there a match for S,_,_ ? check the separate S,_,_ column family
        key.clear();
        encoder.formatSingleNode(key, subject);
        if (db.get(cfhS__, readOptionsInstance, key.flip(), valueBuffer) != RocksDB.NOT_FOUND) {
            return getLabels(valueBuffer, labels);
        }

        // Checking _,P,_
        // Is there a match for _,P,_ ? check the separate _,P,_ column family
        key.clear();
        encoder.formatSingleNode(key, predicate);
        if (db.get(cfh_P_, readOptionsInstance, key.flip(), valueBuffer) != RocksDB.NOT_FOUND) {
            return getLabels(valueBuffer, labels);
        }

        // Checking _,_,_
        // Is there a match for _,_,_ ? check the separate _,_,_ column family
        if (db.get(cfh___, readOptionsInstance, KEY_cfh___, valueBuffer) != RocksDB.NOT_FOUND) {
            return getLabels(valueBuffer, labels);
        }

        // Is it acceptable for _,_,_ to be empty ?
        return List.of();
    }

    @Override
    public Transactional getTransactional() {
        return this.txRocksDB;
    }

    /**
     * Add (or replace) an entry to the labels store keyed by a triple S,P,O
     *
     * @param triple supplied as a triple
     * @param labels to associate with the supplied triple
     */
    @Override
    public void add(Triple triple, List<String> labels) {
        Triple triple2 = tripleNullsToAnyTriple(triple);
        tripleLabelCache.remove(triple2);
        Node s = triple2.getSubject();
        Node p = triple2.getPredicate();
        Node o = triple2.getObject();
        addRule(s, p, o, labels);
    }

    // Convert a triple so that nulls become ANY.
    // Returns the input object if there is no change.
    private static Triple tripleNullsToAnyTriple(Triple triple) {
        Node s = nullToAny(triple.getSubject());
        Node p = nullToAny(triple.getPredicate());
        Node o = nullToAny(triple.getObject());
        if ( s == triple.getSubject() && p == triple.getPredicate() && o == triple.getObject() )
            return triple;
        return Triple.create(s, p, o);
    }

    /**
     * Add (or replace) an entry to the labels store keyed by a triple S,P,O
     *
     * @param subject part of the triple
     * @param property part of the triple
     * @param object part of the triple
     * @param labels to associate with the supplied triple
     */
    @Override
    public void add(Node subject, Node property, Node object, List<String> labels) {
        Node s = nullToAny(subject);
        Node p = nullToAny(property);
        Node o = nullToAny(object);
        // Flush cache because it may have different labels.
        // After standardization.
        tripleLabelCache.remove(Triple.create(s,p,o));
        addRule(s, p, o, labels);
    }

    /**
     * Perform the code of adding a rule by deciding which pattern and column family
     * to write to. This is the single place that all updates happen.
     *
     * @param subject part of the triple
     * @param property part of the triple
     * @param object part of the triple
     * @param labels to associate with the supplied triple
     */
    private void addRule(final Node subject, final Node property, /*final*/ Node object, final List<String> labels) {
        if ( normalizeFunction != null )
            object = normalizeFunction.apply(object) ;
        addRuleWorker(subject, property, object, labels);
    }

    private void addRuleWorker(final Node subject, final Node property, final Node object, final List<String> labels) {

        // Single point for all adding to the labels store.
        if ( db == null )
            throw new RuntimeException("The RocksDB labels store appears to be closed.");
        Labels.LOG.debug("addRule ({},{},{}) -> {}", subject, property, object, labels);

        if ( true ) {
            // Pattern disabled.
            // The machinery does support patterns but the feature is disabled pending reconsideration.
            if ( !subject.isConcrete() || !property.isConcrete() || !object.isConcrete() ) {
                String msg = String.format("Unsupported: triple pattern: %s %s %s",
                                           NodeFmtLib.strTTL(subject),
                                           NodeFmtLib.strTTL(property),
                                           NodeFmtLib.strTTL(object));
            }
        }

        L.validateLabels(labels);

        var pattern = ABACPattern.fromTriple(subject, property, object);
        switch (pattern) {
            case PATTERN_SPO -> addRuleSPO(subject, property, object, labels);
            case PATTERN_SPX -> addRuleSP_(subject, property, labels);
            case PATTERN_SXX -> addRuleS__(subject, labels);
            case PATTERN_XPX -> addRule_P_(property, labels);
            case PATTERN_XXX -> addRule___(labels);
        }
        counts[pattern.ordinal()] += 1;
        var isPattern = pattern != ABACPattern.PATTERN_SPO;
        if ( isPattern )
            this.patternsLoaded = true;
    }

    /**
     * Add a rule for a specific SPO to the SPO column family
     *
     * @param subject part of the triple
     * @param predicate part of the triple
     * @param object part of the triple
     * @param labelsList to associate with the supplied triple
     */
    private void addRuleSPO(final Node subject, final Node predicate, final Node object, final List<String> labelsList) {
        var key = keyBuffer.get().clear();
        encoder.formatTriple(key, subject, predicate, object);
        var labels = labelsBuffer.get().clear();
        encoder.formatStrings(labels, labelsList);
        labelMode.writeUsingMode(txRocksDB, cfhSPO, key.flip(), labels.flip());
    }

    /**
     * Add a rule for SPAny to the SPO column family
     *
     * @param subject part of the triple
     * @param predicate part of the triple
     * @param labelsList to associate with the supplied triple
     */
    private void addRuleSP_(final Node subject, final Node predicate, final List<String> labelsList) {
        var key = keyBuffer.get().clear();
        encoder.formatTriple(key, subject, predicate, Node.ANY);
        var labels = labelsBuffer.get().clear();
        encoder.formatStrings(labels, labelsList);
        labelMode.writeUsingMode(txRocksDB, cfhSPO, key.flip(), labels.flip());
    }

    /**
     * Add a rule for *P* to the predicate-only rules column family
     *
     * @param predicate part of the triple
     * @param labelsList to associate with the supplied predicate
     */
    private void addRule_P_(Node predicate, final List<String> labelsList) {
        var key = keyBuffer.get().clear();
        encoder.formatSingleNode(key, predicate);
        var labels = labelsBuffer.get().clear();
        encoder.formatStrings(labels, labelsList);
        labelMode.writeUsingMode(txRocksDB, cfh_P_, key.flip(), labels.flip());
    }

    /**
     * Add a rule for S** to the subject-only rules column family
     *
     * @param subject part of the triple
     * @param labelsList to associate with the supplied subject
     */
    private void addRuleS__(Node subject, final List<String> labelsList) {
        var key = keyBuffer.get().clear();
        encoder.formatSingleNode(key, subject);
        var labels = labelsBuffer.get().clear();
        encoder.formatStrings(labels, labelsList);
        labelMode.writeUsingMode(txRocksDB, cfhS__, key.flip(), labels.flip());
    }

    /**
     * Add/update the one-and-only entry in the full-wildcard table
     *
     * @param labelsList to associate with the backstop/wildcard
     */
    private void addRule___(final List<String> labelsList) {
        var labels = labelsBuffer.get().clear();
        encoder.formatStrings(labels, labelsList);
        labelMode.writeUsingMode(txRocksDB, cfh___, KEY_cfh___, labels.flip());
    }

    /**
     * Remove any labels for a specific triple.
     * This does not affect any patterns.
     */
    @Override
    public void remove(Triple triple) {
        tripleLabelCache.remove(triple);
        throw new UnsupportedOperationException("LabelsStore.remove not supported by LabelsStoreRockDB");
    }

    // Information gathering counts of each pattern type is used.
    // This does not account for duplicates of the same label (they could as two)
    // This indicative count is primarily for development.
    private final long[] counts = new long[ABACPattern.values().length];

    @Override
    public Graph asGraph() {
        return Graph.emptyGraph;
// Graph graph = L.labelsToGraph(this);
// return graph;
    }

    /**
     * The properties returned by the RocksDB labels store include a number of
     * metrics. {@code size} is accurate but expensive to calculate; {@code count} is
     * maintained by counting insertions using this instance of the store
     * and is an approximate metric which RocksDB can calculate
     * efficiently.
     *
     * @return the properties of the RocksDB labels store
     */
    @Override
    public Map<String, String> getProperties() {
        final var properties = new HashMap<String, String>();

        properties.put("approxsize", "" + approximateSizes());
        properties.put("size", "" + expensiveCount());
        for ( ABACPattern pattern : ABACPattern.values() ) {
            properties.put("count" + pattern, "" + counts[pattern.ordinal()]);
        }

        if ( Labels.LOG.isDebugEnabled() ) {
            properties.put("keyTotalSize", "" + keyTotalSize.get());
            properties.put("valueTotalSize", "" + valueTotalSize.get());
        }

        return properties;
    }

    private long approximateSizes() {
        var first = new byte[0];
        var last = new byte[16];
        Arrays.fill(last, (byte)0xff);

        long sizes = 0;
        sizes += getApproximateSize(cfhSPO, first, last);
        sizes += getApproximateSize(cfhS__, first, last);
        sizes += getApproximateSize(cfh_P_, first, last);
        var afterCFH___ = new byte[1];
        afterCFH___[0] = (byte)(KEY_cfh___.get(0) + 1);
        sizes += getApproximateSize(cfh___, new byte[0], afterCFH___);

        return sizes;
    }

    /**
     * @param cfh column family to get size of
     * @param first must be a logically valid key in the keys of cfh (doesn't have to
     *     be in the store)
     * @param last must be a logically valid key in the keys of cfh (doesn't have to
     *     be in the store)
     * @return a number representing an approximate size (the best effort)
     */
    private long getApproximateSize(ColumnFamilyHandle cfh, byte[] first, byte[] last) {
        final long[] sizes = db.getApproximateSizes(cfh, List.of(new Range(new Slice(first), new Slice(last))),
                                                    SizeApproximationFlag.INCLUDE_FILES, SizeApproximationFlag.INCLUDE_MEMTABLES);

        if ( sizes.length != 1 ) {
            throw new RuntimeException("Unexpected size range of RocksDB column family: " + sizes.length);
        }
        return sizes[0];
    }

    private long expensiveCount() {
        var count = 0;
        for ( var cfh : List.of(cfhSPO, cfhS__, cfh_P_, cfh___) ) {
            count += getExpensiveCount(cfh);
        }
        return count;
    }

    private int getExpensiveCount(ColumnFamilyHandle cfh) {
        try (var it = db.newIterator(cfh)) {
            var count = 0;
            for ( it.seekToFirst() ; it.isValid() ; it.next() ) {
                count++;
            }
            return count;
        }
    }

    private boolean columnFamilyIsEmpty(ColumnFamilyHandle cfh) {
        try (var it = db.newIterator(cfh)) {
            it.seekToFirst();
            return (!it.isValid());
        }
    }

    @Override
    public boolean isEmpty() {
        for ( var cfh : List.of(cfhSPO, cfhS__, cfh_P_, cfh___) ) {
            if ( !columnFamilyIsEmpty(cfh) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns every distinct label ever written to this store, gathered by scanning all
     * column families (SPO, S__, _P_, ___) and parsing each entry's stored label list.
     * <p>
     * This is a full scan - expensive on a large store - but is only invoked once per
     * OPA decision request to build the vocabulary sent alongside the request (see
     * {@link uk.gov.dbt.ndtp.jena.abac.lib.OpaDatasetFilterProvider}), not on the
     * per-triple label lookup hot path.
     */
    @Override
    public Set<String> distinctLabels() {
        if (db == null) {
            throw new RuntimeException("The RocksDB labels store appears to be closed.");
        }

        Set<String> distinct = new HashSet<>();
        for (ColumnFamilyHandle cfh : allColumnFamilies()) {
            try (RocksIterator it = db.newIterator(cfh)) {
                for (it.seekToFirst(); it.isValid(); it.next()) {
                    ByteBuffer valueBuffer = ByteBuffer.wrap(it.value()).order(ByteOrder.LITTLE_ENDIAN);
                    while (valueBuffer.position() < valueBuffer.limit()) {
                        parser.parseStrings(valueBuffer, distinct);
                    }
                }
            }
        }
        return distinct;
    }

    /**
     * Invoke compaction on the underlying RocksDB
     * <p>
     * After compaction, the RocksDB database should have a more predictable and
     * consistent performance and storage footprint for the workload it has been
     * applied to.
     */
    public void compact() {
        Labels.LOG.info("RocksDB label store: perform compaction");
        try {
            for ( var cfh : allColumnFamilies() ) {
                var from = new byte[]{};
                var to = new byte[16];
                for ( int i = 0 ; i < 16 ; i++ )
                    to[i] = (byte)-1;
                db.compactRange(cfh, from, to);
            }
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        if ( openFlag.compareAndSet(true, false) ) {
            try {
                db.closeE();
            } catch ( RocksDBException rocksDBException) {
                Labels.LOG.error("Problem encountered closing RocksDB instance", rocksDBException);
            }
        }

        db = null;
        // RocksDB knows which cfh(s) it owns, and closes them as part of db.close(),
        // so we don't have to close them.
        // But just in case the now-closed CFs contain dangling references to
        // de-allocated C++ structures,
        // we forget the references.
        cfhSPO = null;
        cfhS__ = null;
        cfh_P_ = null;
        cfh___ = null;
    }

    /**
     * Back up the underlying Rocks DB instance to given path
     * @param path location of backup
     */
    public void backup(String path) {
        // Create a backup engine
        try (BackupEngine backupEngine = BackupEngine.open(db.getEnv(), new BackupEngineOptions(path))) {
            Labels.LOG.info("Backing Up Labels Store (begin): {}", path);
            backupEngine.createNewBackup(db, true);
            Labels.LOG.info("Backing Up Labels Store (finished): {}", path);
        } catch (Exception exception) {
            Labels.LOG.error("Backing Up Labels Store (failed)", exception);
            throw new LabelsException("Backup Labels Store failed", exception);
        }
    }

    /**
     * Replace existing Rocks DB instance with given path.
     * @param path location of backup
     */
    public void restore(String path) {
        // Create a backup engine
        try (BackupEngine backupEngine = BackupEngine.open(db.getEnv(), new BackupEngineOptions(path))) {
            Labels.LOG.info("Restoring Labels Store (begin): {}", path);
            Labels.LOG.info("Restoring Labels Store (closing DB): {}", db.isClosed());
            close();
            Labels.LOG.info("Restoring Labels Store (from backup)");
            backupEngine.restoreDbFromLatestBackup(this.dbPath, path, new RestoreOptions(false));
            Labels.LOG.info("Restoring Labels Store (re-opening DB)");
            openDB();
            Labels.LOG.info("Restoring Labels Store (clearing cache): {}", tripleLabelCache.size());
            tripleLabelCache.clear();
            Labels.LOG.info("Restoring Labels Store (cache cleared): {}", tripleLabelCache.size());
            Labels.LOG.info("Restoring Labels Store (finished): {}", path);
        } catch (Exception exception) {
            Labels.LOG.error("Restoring Labels Store (failed)", exception);
            throw new LabelsException("Restore Labels Store failed", exception);
        }
    }
}
