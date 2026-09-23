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
import org.apache.jena.graph.NodeFactory;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

/**
 *
 *  A string-based implementation of a storage format for {@code RocksDB}-based label stores.
 *  <p>
 *  Format labels and triples for low level storage. Nodes are encoded using their contents.
 *  This results in a greater use of space in the database instance ({@code RocksDB}) than for
 *  id-based encoding, but makes the storage format independent of the indirection and cacheing
 *  requirements of an id-base encoding.
 */
public class StoreFmtByString implements StoreFmt {

    /**
     * Object describing a node, and with a very simple encoding of itself to storage
     */
    static class NodeInfo {
        NodeType nodeType;
        int nodeSize;

        NodeInfo(Node node, int nodeSize) {
            this.nodeType = NodeType.of(node);
            this.nodeSize = nodeSize;
        }

        NodeInfo(NodeType nodeType, int nodeSize) {
            this.nodeType = nodeType;
            this.nodeSize = nodeSize;
        }

        void encode(ByteBuffer byteBuffer) {
            byteBuffer.put((byte) nodeType.ordinal());
            StoreFmt.encodeInt(byteBuffer, nodeSize);
        }

        static NodeInfo decode(ByteBuffer byteBuffer) {
            var nodeType = NodeType.values()[byteBuffer.get()];
            var nodeSize = StoreFmt.decodeInt(byteBuffer);

            return new NodeInfo(nodeType, nodeSize);
        }

        //Number of bytes we use to encode this
        final static int NODEINFO_SIZE = 5;
    }

    /**
     * The preamble to a triple's encoding on storage is a {@link NodeInfo}
     * for each of the node components of the triple.
     */
    static class Preamble {
        NodeInfo subject;
        NodeInfo predicate;
        NodeInfo object;

        void encode(ByteBuffer byteBuffer) {
            subject.encode(byteBuffer);
            predicate.encode(byteBuffer);
            object.encode(byteBuffer);
        }

        void decode(ByteBuffer byteBuffer) {
            subject = NodeInfo.decode(byteBuffer);
            predicate = NodeInfo.decode(byteBuffer);
            object = NodeInfo.decode(byteBuffer);
        }

        //Number of bytes we use to encode this
        final static int PREAMBLE_SIZE = NodeInfo.NODEINFO_SIZE * 3;
    }

    /**
     * Parser complement to the encoder for string-based encoding of a label store
     */
    static class Parser implements StoreFmt.Parser {

        private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

        /**
         * Parse the format of key used in SPO or SP_
         *
         * @param byteBuffer from which the triple of nodes are to be parsed
         * @param spo list to which to add resulting nodes
         *
         * @return the original formatter, moved forward on its bytebuffer - 3 nodes are added to the input list
         */
        @Override
        public Parser parseTriple(final ByteBuffer byteBuffer, final List<Node> spo) {
            var preamble = new Preamble();
            preamble.decode(byteBuffer);
            spo.add(parseNode(byteBuffer, preamble.subject));
            spo.add(parseNode(byteBuffer, preamble.predicate));
            spo.add(parseNode(byteBuffer, preamble.object));
            return this;
        }

        @Override
        public StoreFmt.Parser parseStrings(ByteBuffer valueBuffer, final Collection<String> labels) {
            StoreFmt.parseStrings(valueBuffer, decoder, labels);
            return this;
        }

        /**
         * Parse a single node based on a piece of NodeInfo (most usually, just previously parsed)
         * @param byteBuffer from which the node is to be parsed
         * @param nodeInfo describing the type of node to expect, and thus how to interpret the contents of the buffer
         * @return the node retrieved by parsing the buffer according to the supplied nodeInfo
         */
        private Node parseNode(final ByteBuffer byteBuffer, final NodeInfo nodeInfo) {

            return switch (nodeInfo.nodeType) {
                case ANY -> Node.ANY;
                case URI -> NodeFactory.createURI(StoreFmt.parseString(byteBuffer, decoder, nodeInfo.nodeSize));
                case LITERAL -> NodeFactory.createLiteralString(StoreFmt.parseString(byteBuffer, decoder, nodeInfo.nodeSize));
                case BLANK -> NodeFactory.createBlankNode(StoreFmt.parseString(byteBuffer, decoder, nodeInfo.nodeSize));
            };
        }

        /**
         * Parse the format of key used in S__ or _P_ - a single node
         *
         * @return a new node
         */
        @Override
        public Node parseSingleNode(final ByteBuffer byteBuffer) {
            var nodeInfo = NodeInfo.decode(byteBuffer);
            return parseNode(byteBuffer, nodeInfo);
        }
    }

    /**
     * Encoder for string-based encoding of a label store
     */
    static class Encoder implements StoreFmt.Encoder {

        Encoder() {}

        @Override
        public Encoder formatStrings(final ByteBuffer byteBuffer, final List<String> strings) {

            StoreFmt.formatStrings(byteBuffer, strings);
            return this;
        }

        /**
         * Write an SPO-triple to the buffer, in a known format
         * <p>
         * A preamble is created which has offsets for the complement nodes
         * and describes if they are nodes, or wildcards,..
         *
         * @param byteBuffer to write the encoding into
         * @param subject first object of the triple to format
         * @param predicate second object
         * @param object third object
         *
         * @return fluent return of this formatter
         */
        @Override
        public Encoder formatTriple(final ByteBuffer byteBuffer, final Node subject, final Node predicate, final Node object) {

            var savedPreamblePosition = byteBuffer.position();
            var preamble = new Preamble();

            // make space for the preamble at the start of the buffer
            var pos = Preamble.PREAMBLE_SIZE + savedPreamblePosition;
            byteBuffer.position(pos);
            var writer = new StoreFmt.Writer(byteBuffer, StandardCharsets.UTF_8.newEncoder());

            // write each of S,P,O and record their positions in the preamble info
            StoreFmt.encodeNode(writer, subject);
            preamble.subject = new NodeInfo(subject, byteBuffer.position() - pos);
            pos = byteBuffer.position();
            StoreFmt.encodeNode(writer, predicate);
            preamble.predicate = new NodeInfo(predicate, byteBuffer.position() - pos);
            pos = byteBuffer.position();
            StoreFmt.encodeNode(writer, object);
            preamble.object = new NodeInfo(object, byteBuffer.position() - pos);

            // Go back and write the preamble to the buffer
            var savedEndPosition = byteBuffer.position();
            byteBuffer.position(savedPreamblePosition);
            preamble.encode(byteBuffer);
            byteBuffer.position(savedEndPosition);

            return this;
        }

        /**
         * Write a single node to the buffer, in a known format
         * <p>
         * Used in the case where only a single node is expected, rather than a triple,
         * i.e. column families indexed by a single node, such as predicate alone.
         *
         * @param byteBuffer to write the encoding into
         * @param node to encode into the buffer
         * @return fluently return this encoder
         */
        @Override
        public Encoder formatSingleNode(final ByteBuffer byteBuffer, final Node node) {

            var savedNodeInfoPosition = byteBuffer.position();

            // make space for the nodeinfo at the start of the buffer
            var pos = NodeInfo.NODEINFO_SIZE + savedNodeInfoPosition;
            byteBuffer.position(pos);
            var writer = new StoreFmt.Writer(byteBuffer, StandardCharsets.UTF_8.newEncoder());

            // write each of S,P,O and record their positions in the preamble info
            StoreFmt.encodeNode(writer, node);
            var nodeInfo = new NodeInfo(node, byteBuffer.position() - pos);

            // Go back and write the preamble to the buffer
            var savedEndPosition = byteBuffer.position();
            byteBuffer.position(savedNodeInfoPosition);
            nodeInfo.encode(byteBuffer);
            byteBuffer.position(savedEndPosition);

            return this;
        }

    }

    @Override
    public StoreFmt.Encoder createEncoder() {
        return new Encoder();
    }

    @Override
    public StoreFmt.Parser createParser() {
        return new Parser();
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

}
