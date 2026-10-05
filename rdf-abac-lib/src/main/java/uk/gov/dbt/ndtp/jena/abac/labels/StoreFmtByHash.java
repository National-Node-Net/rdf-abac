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

import uk.gov.dbt.ndtp.jena.abac.labels.hashing.Hasher;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Node;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

/**
 *
 *  An id-based implementation of a storage format for {@code RocksDB}-based label stores.
 *  <p>
 *  Because it is using a hash to generate the ID, it is a one way process and so for processing Labels,
 *  we will use the existing String parsing {@code parseStrings()}.
 */
public class StoreFmtByHash implements StoreFmt {

    private final Hasher hasher;

    public StoreFmtByHash(Hasher hasher) {
        this.hasher = hasher;
    }

    @Override
    public Encoder createEncoder() {
        return new HashEncoder(hasher);
    }

    @Override
    public Parser createParser() {
        return new OnlyStringParser();
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

    static String encodeNodeAsString(Node node) {
        return switch (NodeType.of(node)) {
            case ANY -> "*";
            case URI -> node.getURI();
            case LITERAL -> node.getLiteral().getLexicalForm();
            case BLANK -> node.getBlankNodeLabel();
        };
    }

    public class HashEncoder implements Encoder {
        public final Hasher hasher;

        public HashEncoder(Hasher hasher) {
            this.hasher = hasher;
        }

        @Override
        public Encoder formatSingleNode(ByteBuffer byteBuffer, Node node) {
            String stringRepresentation = encodeNodeAsString(node);
            byte[] byteRepresentation = hashInput(stringRepresentation);
            byteBuffer.put(byteRepresentation);
            return this;
        }

        @Override
        /*
         * This is only used to encode the labels themselves thus we will not hash it.
         * It needs to be a reversible action.
         */
        public Encoder formatStrings(ByteBuffer byteBuffer, List<String> strings) {
            StoreFmt.formatStrings(byteBuffer, strings);
            return this;
        }

        @Override
        public Encoder formatTriple(ByteBuffer byteBuffer, Node subject, Node predicate, Node object) {
            formatSingleNode(byteBuffer, subject);
            formatSingleNode(byteBuffer, predicate);
            formatSingleNode(byteBuffer, object);

            return this;
        }

        private byte[] hashInput(String input) {
            return hasher.hash(input);
        }
    }

    /**
     * This class will only ever parse Strings - since it's reversible;
     * as the hashing functions used in encoding are otherwise one-way only.
     */
    public static class OnlyStringParser implements Parser {

        private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

        @Override
        /* NO-OP */
        public Node parseSingleNode(ByteBuffer byteBuffer) {
            throw new NotImplemented();
        }

        @Override
        /* NO-OP */
        public Parser parseTriple(ByteBuffer byteBuffer, List<Node> spo) {
            throw new NotImplemented();
        }

        @Override
        public Parser parseStrings(ByteBuffer valueBuffer, Collection<String> labels) {
            StoreFmt.parseStrings(valueBuffer, decoder, labels);
            return this;
        }
    }
}
