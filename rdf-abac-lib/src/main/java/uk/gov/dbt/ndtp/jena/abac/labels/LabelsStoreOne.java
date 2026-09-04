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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.sparql.core.TransactionalLock;
import org.apache.jena.sparql.graph.GraphZero;

/**
 * An immutable labels store with only one setting, fixed when created.
 * All looks return that fixed label.
 */
public class LabelsStoreOne implements LabelsStore {

    private final List<String> labels;
    private final Transactional transactional = TransactionalLock.createMRPlusSW();

    /*package*/ LabelsStoreOne(String label) {
        this.labels = (label != null) ? List.of(label) : List.of();
    }

    @Override
    public Transactional getTransactional() {
        return transactional;
    }

    @Override
    public List<String> labelsForTriples(Triple triple) {
        if ( ! triple.isConcrete() ) {
            Log.error(Labels.class, "Asked for labels for a triple with wildcards: "+NodeFmtLib.displayStr(triple));
            return null;
        }
        return labels;
    }

    @Override
    public void add(Triple triple, List<String> labels) {
        throw new UnsupportedOperationException("Can't add to LabelsStoreOne");
    }

    @Override
    public void add(Node subject, Node Property, Node object, List<String> labels) {
        throw new UnsupportedOperationException("Can't add to LabelsStoreOne");
    }

    @Override
    public void add(Graph labels) {
        throw new UnsupportedOperationException("Can't load into LabelsStoreOne");
    }

    @Override
    public void remove(Triple triple) {
        throw new UnsupportedOperationException("Can't remove from LabelsStoreOne");
    }

    @Override
    public boolean isEmpty() { return true; }

    @Override
    public Graph asGraph() { return GraphZero.instance(); }

    @Override
    public Map<String, String> getProperties() {
        return new HashMap<>();
    }

    @Override
    public void forEach(BiConsumer<Triple, List<String>> action) {
        action.accept(Triple.ANY, labels);
    }

    @Override
    public Set<String> distinctLabels() {
        return Set.copyOf(labels);
    }
}
