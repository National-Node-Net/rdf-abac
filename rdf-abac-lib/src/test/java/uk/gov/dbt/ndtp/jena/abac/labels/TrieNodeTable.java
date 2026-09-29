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

package uk.gov.dbt.ndtp.jena.abac.labels;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.graph.Node;
import org.apache.jena.tdb2.store.NodeId;
import org.apache.jena.tdb2.store.NodeIdFactory;
import org.apache.jena.tdb2.store.nodetable.NodeTable;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Trie-based node table for testing
 * <p>
 * More space-efficient than {@link NaiveNodeTable}, but a bit slower.
 * Prefer this for testing the loading of very large volumes of data to the label store.
 * <p>
 * Reverse lookup {@link NodeTable#getNodeForNodeId(NodeId)} (id to node) is not implemented.
 */
public class TrieNodeTable implements NodeTable {

    final AtomicLong idIndex = new AtomicLong();

    final TrieNodeMap idTable = new TrieNodeMap(idIndex::incrementAndGet);

    @Override
    public NodeId getAllocateNodeId(Node node) {
        return NodeIdFactory.createPtr(idTable.add(node));
    }

    @Override
    public NodeId getNodeIdForNode(Node node) {
        return null;
    }

    @Override
    public Node getNodeForNodeId(NodeId nodeId) {
        return null;
    }

    @Override
    public boolean containsNode(Node node) {
        return false;
    }

    @Override
    public boolean containsNodeId(NodeId nodeId) {
        return false;
    }

    @Override
    public List<NodeId> bulkNodeToNodeId(List<Node> list, boolean b) {
        return null;
    }

    @Override
    public List<Node> bulkNodeIdToNode(List<NodeId> list) {
        return null;
    }

    private static class WrapNodeMapIterator implements Iterator<Pair<NodeId, Node>> {

        WrapNodeMapIterator(Iterator<Pair<Node, Long>> nodeMapIterator) {
            this.nodeMapIterator = nodeMapIterator;
        }

        Iterator<Pair<Node, Long>> nodeMapIterator;

        @Override
        public boolean hasNext() {
            return nodeMapIterator.hasNext();
        }

        @Override
        public Pair<NodeId, Node> next() {
            var internal = nodeMapIterator.next();
            return Pair.create(NodeIdFactory.createPtr(internal.cdr()), internal.car());
        }
    }

    @Override
    public Iterator<Pair<NodeId, Node>> all() {

        return new WrapNodeMapIterator(idTable.iterator());
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public NodeTable wrapped() {
        return null;
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sync() {
        throw new UnsupportedOperationException();
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
