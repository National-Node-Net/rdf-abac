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

import org.apache.jena.graph.Node;
import org.apache.jena.tdb2.store.NodeIdFactory;
import org.apache.jena.tdb2.store.nodetable.NodeTable;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

/**
 *
 *  An id-based implementation of a storage format for {@code RocksDB}-based label stores.
 *  <p>
 *  Because it is id-based, this format receives and uses a {@link NodeTable} which it uses
 *  to convert from nodes to ids as a step in formatting nodes into a RocksDB database.
 */
public class StoreFmtByNodeId implements StoreFmt {
    @Override
    public Encoder createEncoder() {
        return new Encoder();
    }

    @Override
    public Parser createParser() {
        return new Parser();
    }

    public StoreFmtByNodeId(final NodeTable storeNodeTable) {
        this.nodeTable = storeNodeTable;
    }

    /**
     * To aid with testing - provide a name
     * @return a toString()
     */
    @Override
    public String toString() {
        String className = getClass().toString();
        int lastDotIndex = className.lastIndexOf('.');
        if (lastDotIndex >= 0) {
            className = className.substring(lastDotIndex + 1);
        }
        return className;
    }


    private final NodeTable nodeTable;

    /**
     * Encoder for id-based encoding of a label store
     */
    class Encoder implements StoreFmt.Encoder {

        /**
         * byte 0 : 7 ................ 4  3 ............................. 0
         *          ordinal of node type  ordinal of IntSize of the node id
         * - that is all, if the node being encoded is a Node.Any
         * - otherwise,
         * byte 1..n as many bytes of integer as are encoded in the IntSize
         * 1..1 for IntSize.OneByte,
         * 1..2 for IntSize.TwoBytes,
         * 1..4 for IntSize.FourBytes,
         * 1..8 for IntSize.EightBytes
         * so a URI node with a long node-id takes up 9 bytes.
         * <p>
         * The main intent is that graphs where the node-ids fit within 32 bits are encoded
         * somewhat more space-efficiently than they would otherwise be,
         * without actually restricting the ids to 32 bits.
         *
         * @param byteBuffer to write the node('s id) to
         * @param node to format
         * @return the original encoder
         */
        @Override
        public Encoder formatSingleNode(ByteBuffer byteBuffer, Node node) {

            var nodeType = NodeType.of(node);
            int topByte = nodeType.ordinal() << 4;
            var pos = byteBuffer.position();
            byteBuffer.position(pos + 1);
            switch (nodeType) {
                case ANY:
                    break;
                case URI:
                case LITERAL:
                case BLANK:
                    var nodeId = nodeTable.getAllocateNodeId(node);
                    topByte = topByte | StoreFmt.formatLongVariable(byteBuffer, nodeId.getPtrLocation()).ordinal();
            }
            byteBuffer.put(pos, (byte) topByte);

            return this;
        }

        /**
         * Format a list of strings
         * <p>
         * As the values encoded for the (key,value)-pairs, these are encoded the same for both id-based
         * and string-based {@code StoreFmt}
         * </p>
         * @param byteBuffer into which to encode the list of strings
         * @param strings to encode
         * @return fluently return the encoder being used
         */
        @Override
        public Encoder formatStrings(final ByteBuffer byteBuffer, final List<String> strings) {

            StoreFmt.formatStrings(byteBuffer, strings);
            return this;
        }

        /**
         * Format a triple using an id-based encoder
         * <p>
         * id-based encoding is much simpler than string-based. Once each constituent node has been looked up in the
         * {@link NodeTable} we simple have to encode that node's id.
         * </p>
         * @param byteBuffer into which to encode the triple
         * @param subject node of the triple
         * @param predicate node of the triple
         * @param object node of the triple
         * @return fluently return the encoder being used
         */
        @Override
        public Encoder formatTriple(ByteBuffer byteBuffer, Node subject, Node predicate, Node object) {
            formatSingleNode(byteBuffer, subject);
            formatSingleNode(byteBuffer, predicate);
            formatSingleNode(byteBuffer, object);

            return this;
        }
    }

    /**
     * Parser for id-based encoding of a label store
     */
    class Parser implements StoreFmt.Parser {

        private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

        /**
         * Reverse the id-based encoding of a single node
         * <p>
         * based on the format beginning:
         * byte 0 : 7 ................ 4  3 ............................. 0
         *           ordinal of node type  ordinal of IntSize of the node id
         *
         * @param byteBuffer containing the encoded node id
         * @return a node created by looking up the decoded id in the {@link NodeTable}
         */
        @Override
        public Node parseSingleNode(final ByteBuffer byteBuffer) {

            var topByte = byteBuffer.get();
            var nodeTypeOrdinal = (topByte >> 4) & 0xf;
            var nodeType = NodeType.values()[nodeTypeOrdinal];
            switch (nodeType) {
                case ANY:
                    return Node.ANY;
                case BLANK:
                case LITERAL:
                case URI:
                    var intBytes = IntBytes.values()[topByte & 0xf];
                    var nodeId = NodeIdFactory.createPtr(StoreFmt.parseLongVariable(byteBuffer, intBytes));
                    return nodeTable.getNodeForNodeId(nodeId);
                default:
                    throw new UnsupportedOperationException("Unexpected NodeType case: " + nodeType);
            }
        }

        /**
         * Parse a triple consisting of 3 consecutive encoded node-ids
         *
         * @param byteBuffer containing the encoded node ids
         * @param spo a list to receive the resulting recreated nodes
         * @return fluently return the parser being used
         */
        @Override
        public Parser parseTriple(final ByteBuffer byteBuffer, final List<Node> spo) {
            spo.add(parseSingleNode(byteBuffer));
            spo.add(parseSingleNode(byteBuffer));
            spo.add(parseSingleNode(byteBuffer));
            return this;
        }

        /**
         * Parse value (label) strings, using the common value string format shared between id-based and string-based
         * store formats.
         *
         * @param valueBuffer the buffer holding the encoded strings
         * @param labels a list to receive the resulting strings
         * @return fluently return the parser being used
         */
        @Override
        public Parser parseStrings(final ByteBuffer valueBuffer, final Collection<String> labels) {
            StoreFmt.parseStrings(valueBuffer, decoder, labels);
            return this;
        }
    }
}
