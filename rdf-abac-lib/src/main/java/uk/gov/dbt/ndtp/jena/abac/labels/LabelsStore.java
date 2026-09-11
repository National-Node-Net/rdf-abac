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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.system.Txn;

/**
 * {@link LabelsStore}s provide a triple to label mapping.
 * <p>
 * They do not support pattern matching; although they can store the pattern and provide it's labels.
 */
public interface LabelsStore { // extends Transactional {

    /**
     * Lookup the triple and return the labels associated with it.
     */
    public List<String> labelsForTriples(Triple triple);

    /**
     * A {@link Transactional} that protects the label store.
     */
    public Transactional getTransactional();

    /** A concrete or pattern triple */
    public default void add(Triple triple, String label) {
        add(triple, List.of(label));
    }

    /** Labels for a specific triple. */
    public void add(Triple triple, List<String> labels);

    /** Labels for a specific triple. */
    public default void add(Node subject, Node property, Node object, String label) {
        add(subject, property, object, List.of(label));
    }

    /** Labels for a specific triple. */
    public void add(Node subject, Node property, Node object, List<String> labels);

    /**
     * Add a graph of label descriptions to the store.
     * @deprecated Use {#addGraph}.
     */
    @Deprecated
    public default void add(Graph labelsData) {
        addGraph(labelsData);
    }

    /**
     * Add a graph of label descriptions to the store.
     * This is done inside a transaction.
     * @see L#loadStoreFromGraph
     */
    public default void addGraph(Graph labelsData) {
        Txn.executeWrite(getTransactional(), ()->L.loadStoreFromGraph(this, labelsData) );
    }

    /**
     * Remove any labels for a specific triple.
     * This does not affect any patterns,
     * only removing labels for a specific triple.
     */
    public void remove(Triple triple);

    /** Is the store empty? */
    public boolean isEmpty();

    /**
     * Apply BiConsumer to each entry in the labels store.
     */
    public void forEach(BiConsumer<Triple, List<String>> action);

    /**
     * The distinct labels held by this store. Excludes the dataset default label; see
     * {@link Labels#vocabulary(LabelsStore, String)} for the complete vocabulary.
     *
     * @throws UnsupportedOperationException if this store cannot enumerate its labels
     */
    public default Set<String> distinctLabels() {
        throw new UnsupportedOperationException(getClass().getSimpleName()+".distinctLabels");
    }

    /**
     * Get labels as graph. This is a development and deployment helper; it may not
     * be supported by all store implementations The graph may be very large. Returns
     * a copy of the labels graph, so it is not connected to the LabelsStore.
     * Returns null if not supported.
     */
    public Graph asGraph();

    /** @deprecated Use {@link #asGraph} */
    @Deprecated
    public default Graph getGraph() { return asGraph(); }

    /**
     * A collection of implementation-dependent values which may be used when testing
     * a {@code LabelStore} implementation.
     *
     * @return the store properties for this labels store implementation
     */
    public Map<String, String> getProperties();
}
