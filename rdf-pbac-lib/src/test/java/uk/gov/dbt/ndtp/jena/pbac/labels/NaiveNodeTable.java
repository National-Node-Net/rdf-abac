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

package uk.gov.dbt.ndtp.jena.pbac.labels;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.graph.Node;
import org.apache.jena.tdb2.store.NodeId;
import org.apache.jena.tdb2.store.NodeIdFactory;
import org.apache.jena.tdb2.store.nodetable.NodeTable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Trivial node table for testing
 * <p>
 * By consequence of being trivial it is fairly fast to add or lookup,
 * but very space inefficient, so that loading very large volumes of data will run out of memory.
 */
public class NaiveNodeTable implements NodeTable {

    final Map<Node, NodeId> idTable = new HashMap<>();
    final Map<NodeId, Node> nodeTable = new HashMap<>();
    final AtomicLong idIndex = new AtomicLong();

    @Override
    public NodeId getAllocateNodeId(Node node) {
        synchronized (this) {
            var found = idTable.get(node);
            if (found != null) {
                return found;
            }

            var id = NodeIdFactory.createPtr(idIndex.incrementAndGet());
            idTable.put(node, id);
            nodeTable.put(id, node);

            return id;
        }
    }

    @Override
    public NodeId getNodeIdForNode(Node node) {
        synchronized (this) {
            return idTable.get(node);
        }
    }

    @Override
    public Node getNodeForNodeId(NodeId nodeId) {
        synchronized (this) {
            return nodeTable.get(nodeId);
        }
    }

    @Override
    public boolean containsNode(Node node) {
        synchronized (this) {
            return idTable.containsKey(node);
        }
    }

    @Override
    public boolean containsNodeId(NodeId nodeId) {
        synchronized (this) {
            return nodeTable.containsKey(nodeId);
        }
    }

    @Override
    public List<NodeId> bulkNodeToNodeId(List<Node> list, boolean b) {
        throw new RuntimeException("Not implemented - List<NodeId> bulkNodeToNodeId(List<Node> list, boolean b)");
    }

    @Override
    public List<Node> bulkNodeIdToNode(List<NodeId> list) {
        throw new RuntimeException("Not implemented - List<Node> bulkNodeIdToNode(List<NodeId> list)");
    }

    @Override
    public Iterator<Pair<NodeId, Node>> all() {
        return nodeTable.entrySet().stream().map(entry -> Pair.create(entry.getKey(), entry.getValue())).iterator();
    }

    @Override
    public boolean isEmpty() {
        synchronized (this) {
            return idTable.isEmpty();
        }
    }

    @Override
    public NodeTable wrapped() {
        return null;
    }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public void sync() {
        // nothing to do
    }

    @Override
    public String toString() {
        String className = getClass().getName();
        int lastDotIndex = className.lastIndexOf('.');
        if (lastDotIndex >= 0) {
            className = className.substring(lastDotIndex + 1);
        }
        return className;
    }
}
