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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import uk.gov.dbt.ndtp.jena.abac.SysABAC;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.sparql.core.TransactionalNull;
import org.apache.jena.system.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Temporary. Previous LabelsStoreMem, with support for patterns.
@Deprecated
public class LabelsStoreMemPattern implements LabelsStore {

    private static final Logger LOG = LoggerFactory.getLogger(Labels.class);

    // Triple indexing.
    // This is not protected by the transactional.
    // It is thread-safe but may be read inconsistently.
    private final Map<Triple, List<String>> tripleLabels = new ConcurrentHashMap<>();

    // Update accumulator used to collect updates which are then flushed to tripleLabels on commit.
    // This allows for a data load operation to abort. The accumulator is flushed
    // after data has been loaded into the triple store, so it is syntactically valid,
    // and before the data is committed and becomes visible.
    //
    // We prefer better availability (writes do not block readers) over consistency.
    // Consistency is minimised for slow updates by use of the accumulator.

    private final Map<Triple, List<String>> accTripleLabels = new ConcurrentHashMap<>();

    private boolean hasPatterns = false;
    private PatternsIndex patternIndex = new PatternsIndex();

    // Future: Consider binding LabelsStore to the DatasetGraphABAC transactional so
    // that operations on the labels side are also protected.
    // While all operation go through a DatasetGraphABAC, the dataset is MR+SW (reads can overlap writes).
    // We choose availability (see asGraph()) over consistency.

    // Used to give consistent update flushing accumulated triples.
    private final Transactional transactional;

    /**
     * TransactionalNull tracks the transaction state so these
     * calls know if they are part of a write transaction.
     */
    private class TransactionalHook extends TransactionalNull {
        @Override
        public void commit() {
            // transactionMode() reflects the current thread transaction state.
            if ( super.transactionMode() == ReadWrite.WRITE ) {
                // Flush the triple label accumulator to the main store.
                flushAccumulator();
            }
            super.commit();
        }

        @Override
        public void abort() {
            if ( super.transactionMode() == ReadWrite.WRITE )
                clearAccumulator();
            super.abort();
        }
    }

    /** A fresh, empty in-memory {@link LabelsStore} */
    public /* for testing */ static LabelsStore create() {
        LabelsStoreMemPattern store = new LabelsStoreMemPattern();
        return store;
    }

    private LabelsStoreMemPattern() {
        // Called from DatasetGraphABAC in its transaction lifecycle.
        this.transactional = new TransactionalHook();
    }

    @Override
    public Transactional getTransactional() { return transactional; }

    // ---- Read operations ----

    @Override
    public List<String> labelsForTriples(Triple triple) {
        if ( ! triple.isConcrete() ) {
            LOG.error("Asked for labels for a triple with wildcards: {}", NodeFmtLib.displayStr(triple));
            return null;
        }

        try {
            readOperation();
        } catch (AuthzTriplePatternException ex) {
            LOG.error("Failed to update index: {}", ex.getMessage());
            return List.of(SysABAC.DENY_LABEL);
        }

        List<String> x = tripleLabels.get(triple);
        if ( x == null && hasPatterns ) {
            x = patternIndex.match(triple);
        }
        //FmtLog.info(ABAC.LOG, "%s : %s\n", str(triple), x);
        return x==null ? List.of() : x;
    }

    //---- Label by pattern
    private List<String> findPatterns(Triple triple) {
        return patternIndex.match(triple);
    }

    /** Signal a read operation. */
    private void readOperation() {
        // Needed when the store is standalone (e.g. tests!) and there is no transaction lifecycle in use.
        if ( ! transactional.isInTransaction() )
            flushAccumulator();
    }
    /** Signal a write operation. */
    private void writeOperation() {
        // Placeholder method
    }

    @Override
    public void forEach(BiConsumer<Triple, List<String>> action) {
        readOperation();
        tripleLabels.forEach(action);
    }

    @Override
    public Set<String> distinctLabels() {
        readOperation();
        Set<String> labels = new HashSet<>();
        tripleLabels.values().forEach(labels::addAll);
        patternIndex.collectLabels(labels);
        return labels;
    }

    @Override
    public Graph asGraph() {
        readOperation();
        Graph gResult = L.newLabelGraph();
        // Thread-safe, but not guaranteed to be consistent.
        L.labelsToGraph(this, gResult);
        return gResult;
    }

    /**
     * The properties of the default label store include the size of the labels map.
     *
     * @return the properties of the label store
     */
    @Override
    public Map<String, String> getProperties() {
        final var properties = new HashMap<String, String>();
        properties.put("size", "" + tripleLabels.size());
        return properties;
    }

    @Override
    public boolean isEmpty() {
        readOperation();
        return tripleLabels.isEmpty();
    }

    private void flushAccumulator() {
        if ( ! accTripleLabels.isEmpty() ) {
            // Ensure only one thread is emptying the accumulator.
            // While ConcurrentHashMaps are safe, the putAll and clear need to be one step.
            synchronized(this) {
                // This is the only write to the main triples->labels map.
                // accTripleLabels is not protected but either:
                //   Transaction in use and only the write transaction will update accTripleLabels.
                //   Freestanding usage in tests when all work is single threaded.
                tripleLabels.putAll(accTripleLabels);
                clearAccumulator();
            }
        }
    }

    private void clearAccumulator() {
        accTripleLabels.clear();
    }

    // ---- Update operations  ----

    @Override
    public void addGraph(Graph labels) {
        writeOperation();
        if ( transactional.isInTransaction() ) {
            add$(labels);
            return;
        }
        Txn.executeWrite(transactional, ()->add$(labels) );
    }

    private void add$(Graph labelsGraph) {
        // Check the small incoming graph.
        L.checkShape(labelsGraph);
        // Concrete triples only
//        L.loadStoreFromGraph(this, labels);

        // Allow patterns.
        BiConsumer<TriplePattern, List<String>> destination = (pattern, labels) -> addToIndex(pattern, labels);
        L.graphToLabels(labelsGraph, destination);
    }

    // Triple may be a pattern (wildcards), but must be S or SP
    @Override
    public void add(Triple triple, List<String> labels) {
        writeOperation();
        add$(triple, labels);
    }

    /** Add a triple pattern but do not rebuild index. */
    private void add$(Triple triple, List<String> labels) {
        if ( triple.isConcrete() ) {
            L.validateLabels(labels);
            accTripleLabels.put(triple, labels);
            return;
        }

        // Patterns.
        if ( !L.isPatternTriple(triple) )
            throw new AuthzTriplePatternException("Bad triple pattern: "+NodeFmtLib.str(triple));
        TriplePattern triplePattern = TriplePattern.create(triple);
        addToIndex(triplePattern, labels);
    }

    /** Add a triple pattern but do not rebuild index. */
    @Override
    public void add(Node subject, Node property, Node object, List<String> labels) {
        writeOperation();
        Node s = nullToAny(subject);
        Node p = nullToAny(property);
        Node o = nullToAny(object);
        Triple triple = Triple.create(s, p, o);
        add(triple, labels);
    }

    // ---- Patterns ----
    // Only available by loading from a graph.
    // This machinery is placeholder - patterns are not current used

    /** Add to the index. */
    private void addToIndex(TriplePattern pattern, List<String> labels) {
        L.validateLabels(labels);
        if ( pattern.isConcrete() ) {
            addConcreteToIndex(pattern, labels);
            return;
        }
        hasPatterns = true;
        addPatternToIndex(pattern, labels);
    }

    /** Index a single specific triple. */
    private void addConcreteToIndex(TriplePattern pattern, List<String> labels) {
        if ( labels.isEmpty() )
            return;
        Triple triple = pattern.asTriple();
        this.add(triple, labels);
    }

    // ---- Patterns

    /** Index for a pattern, not labels for a single specific triple. */
    private void addPatternToIndex(TriplePattern pattern, List<String> labels) {
        patternIndex.add(pattern, labels);
    }

    /**
     * Remove any labels for a specific triple.
     * This does not affect any patterns,
     * only removing labels for a specific triple lookup.
     */
    @Override
    public void remove(Triple triple) {
        this.tripleLabels.remove(triple);
        this.accTripleLabels.remove(triple);
    }

    @Override
        public String toString() {
            return String.format("%s[%d]", this.getClass().getSimpleName(), tripleLabels.size());
            // Small scale only!
    //        Graph g = getGraph();
    //        return RDFWriter.source(g).lang(Lang.TTL).asString();
        }

}
