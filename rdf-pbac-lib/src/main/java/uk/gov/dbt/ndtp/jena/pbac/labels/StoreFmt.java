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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

/**
 * The storage format interface for {@code RocksDB}-based label stores.
 * There are storage format implementations for string-based and id-based encoding of nodes into buffers,
 * which are passed to RocksDB for storage.
 * <p>
 * This class also provides a number of static utility methods for parts of the encoding and decoding
 * process which do not vary by (string-based versus id-based) storage formats.
 */
public interface StoreFmt {

    /**
     * The ordering of {@code NodeType} enum values was initially defined for {@code Any} to be last in order to respect
     * how wildcards are processed in seeking {@link LabelsStoreRocksDB}.
     * As the ordinal values will be encoded on storage, these MUST NOT BE CHANGED.
     */
    enum NodeType {
        URI, LITERAL, BLANK, ANY;

        static NodeType of(Node node) {
            if (node.isURI()) {
                return NodeType.URI;
            } else if (node.isLiteral()) {
                return NodeType.LITERAL;
            } else if (node.isBlank()) {
                return NodeType.BLANK;
            } else {
                return NodeType.ANY;
            }
        }
    };

    /**
     * Create an encoder that uses this storage format
     * @return a new encoder
     */
    public Encoder createEncoder();

    /**
     * Create a parser that uses this storage format
     * @return a new parser
     */
    public Parser createParser();

    /**
     * The object that transforms triples, nodes and values into sequences of bytes for storage in {@code RocksDB}
     */
    public interface Encoder {

        Encoder formatSingleNode(final ByteBuffer byteBuffer, final Node node);

        Encoder formatStrings(final ByteBuffer byteBuffer, final List<String> strings);

        Encoder formatTriple(final ByteBuffer byteBuffer, final Node subject, final Node predicate, final Node object);

    };

    /**
     * The object that reads sequences of bytes from storage in {@code RocksDB} back into triples, nodes and values.
     */
    public interface Parser {

        Node parseSingleNode(final ByteBuffer byteBuffer);

        Parser parseTriple(final ByteBuffer byteBuffer, final List<Node> spo);

        Parser parseStrings(final ByteBuffer valueBuffer, final Collection<String> labels);
    };

    static void encodeNode(Writer writer, Node node) {

        try {
            switch (NodeType.of(node)) {
                case ANY:
                    break;
                case URI:
                    writer.write(node.getURI());
                    break;
                case LITERAL:
                    writer.write(node.getLiteral().getLexicalForm());
                    break;
                case BLANK:
                    writer.write(node.getBlankNodeLabel());
                    break;
            };
        } catch (IOException e) {
            throw new RuntimeException("Exception when formatting/writing node", e);
        }
    }

    /**
     * Encode an integer in the standard number of bytes for an integer (4)
     * @param byteBuffer
     * @param i the integer
     */
    static void encodeInt(ByteBuffer byteBuffer, int i) {

        assert byteBuffer.order() == ByteOrder.LITTLE_ENDIAN;
        byteBuffer.putInt(i);
    }

    /**
     * Format a number of strings,
     * preceded by a count of the strings, then an array of the sizes of the strings
     *
     * @param byteBuffer target of the encoding
     * @param strings to encode
     */
    static void formatStrings(final ByteBuffer byteBuffer, final List<String> strings) {
        var count = strings.size();
        StoreFmt.encodeInt(byteBuffer, count);
        int[] encodedSizes = new int[count];
        var nextIndex = 0;
        var sizesPosition =  byteBuffer.position();
        // Allow space for the string lengths
        var nextPosition = sizesPosition + 4 * count;
        byteBuffer.position(nextPosition);

        var charsetEncoder = StandardCharsets.UTF_8.newEncoder();
        for (String string : strings) {
            var result = charsetEncoder.encode(CharBuffer.wrap(string), byteBuffer, true);
            if (!result.isUnderflow()) {
                throw new RuntimeException(
                    new IOException("Implementation error - format strings - LabelsRocksFormatter's ByteBuffer should be larger than current limit " + byteBuffer.capacity()));
            }
            encodedSizes[nextIndex++] = byteBuffer.position() - nextPosition;
            nextPosition = byteBuffer.position();
        }

        byteBuffer.position(sizesPosition);
        for (int encodedSize : encodedSizes) {
            StoreFmt.encodeInt(byteBuffer, encodedSize);
        }
        byteBuffer.position(nextPosition);
    }

    /**
     * Parse a single string of known number of bytes encoded in a bytebuffer
     * @param byteBuffer containing the encoded string
     * @param decoder the decoder to use
     * @param len the number of bytes to decode
     * @return String
     */
    static String parseString(final ByteBuffer byteBuffer, final CharsetDecoder decoder, final int len) {
        var pos = byteBuffer.position();
        var slice = byteBuffer.slice(pos, len);
        var chars = CharBuffer.allocate(len);
        decoder.decode(slice, chars, true);
        byteBuffer.position(pos + len);
        return chars.flip().toString();
    }

    /**
     * Parse back a number of strings encoded as (count,lengths[],strings)
     *
     * @param byteBuffer containing the encoded strings
     * @param decoder to use to decode to characters
     * @param strings result list to add decoded strings to
     */
    static void parseStrings(final ByteBuffer byteBuffer, final CharsetDecoder decoder, final Collection<String> strings) {
        var count = StoreFmt.decodeInt(byteBuffer);
        int[] encodedSizes = new int[count];
        for (int i = 0; i < count; i++) {
            encodedSizes[i] = StoreFmt.decodeInt(byteBuffer);
        }

        for (int i = 0; i < count; i++) {
            int len = encodedSizes[i];
            strings.add(StoreFmt.parseString(byteBuffer, decoder, len));
        }
    }


    /**
     * Decode a single 4-byte integer encoded little endianly
     *
     * @param byteBuffer containing the encoded integer
     * @return the integer value retrieved from the bytebuffer
     */
    static int decodeInt(ByteBuffer byteBuffer) {
        assert byteBuffer.order() == ByteOrder.LITTLE_ENDIAN;
        return byteBuffer.getInt();
    }

    enum IntBytes {
        ONE_BYTE, TWO_BYTES, FOUR_BYTES, EIGHT_BYTES;
    }

    /**
     * Format a long value flexibly according to how big it is,
     * as how big it is will be recorded elsewhere (in another part of the encoding, or implicitly).
     *
     * @param byteBuffer to format into
     * @param value the long to format
     * @return how much space the long value takes up.
     */
    static IntBytes formatLongVariable(final ByteBuffer byteBuffer, long value) {

        assert byteBuffer.order() == ByteOrder.LITTLE_ENDIAN;
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            byteBuffer.putLong(value);
            return IntBytes.EIGHT_BYTES;
        } else if (value > Short.MAX_VALUE || value < Short.MIN_VALUE) {
            byteBuffer.putInt((int)value);
            return IntBytes.FOUR_BYTES;
        } else if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
            byteBuffer.putShort((short) value);
            return IntBytes.TWO_BYTES;
        } else {
            byteBuffer.put((byte) value);
            return IntBytes.ONE_BYTE;
        }
    }

    /**
     * parse back into a long, which may have been encoded a byte, short, int or long
     * @param byteBuffer contains the encoded long
     * @param size tells us what the encoding was
     * @return the decoding of the encoded long
     */
    static long parseLongVariable(final ByteBuffer byteBuffer, IntBytes size) {

        assert byteBuffer.order() == ByteOrder.LITTLE_ENDIAN;
        return switch (size) {
            case ONE_BYTE -> byteBuffer.get();
            case TWO_BYTES -> byteBuffer.getShort();
            case FOUR_BYTES -> byteBuffer.getInt();
            case EIGHT_BYTES -> byteBuffer.getLong();
        };
    }

    /**
     * An implementation of {@link java.io.Writer} which writes to a {@link ByteBuffer}
     */
    class Writer extends java.io.Writer {

        Writer(final ByteBuffer byteBuffer, final CharsetEncoder charsetEncoder) {
            this.byteBuffer = byteBuffer;
            this.charsetEncoder = charsetEncoder;
        }

        private final ByteBuffer byteBuffer;
        private final CharsetEncoder charsetEncoder;

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            var charBuffer = CharBuffer.wrap(cbuf, off, len);
            while (true) {
                var pos = byteBuffer.position();
                var result = charsetEncoder.encode(charBuffer, byteBuffer, true);
                if (result.isOverflow()) {
                    // ignore the partial, overflowed encoding, try again in a bigger buffer
                    byteBuffer.position(pos);
                    extendByteBuffer();
                    continue;
                }
                if (!result.isUnderflow()) {
                    throw new IOException("Implementation error - format labels/triples - LabelsRocksFormatter's ByteBuffer should be larger than current limit " + byteBuffer.capacity());
                }
            }
        }

        @Override
        public void write(String s) throws IOException {
            var charBuffer = CharBuffer.wrap(s);
            while (true) {
                var pos = byteBuffer.position();
                var result = charsetEncoder.encode(charBuffer, byteBuffer, true);
                if (result.isOverflow()) {
                    // ignore the partial, overflowed encoding, try again in a bigger buffer
                    byteBuffer.position(pos);
                    extendByteBuffer();
                    continue;
                }
                if (!result.isUnderflow()) {
                    throw new IOException("Implementation error - format labels/triples - LabelsRocksFormatter's ByteBuffer should be larger than current limit " + byteBuffer.capacity());
                }
                break;
            }
        }

        private void extendByteBuffer() {
            throw new RuntimeException("extendByteBuffer() not implemented");
        }

        @Override
        public void flush() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException();
        }
    }
}
