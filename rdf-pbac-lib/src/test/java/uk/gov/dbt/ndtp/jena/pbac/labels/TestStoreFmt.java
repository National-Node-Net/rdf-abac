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

import uk.gov.dbt.ndtp.jena.pbac.labels.hashing.Hasher;
import org.apache.jena.graph.*;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.sse.SSE;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public abstract class TestStoreFmt {

    protected ByteBuffer byteBuffer;
    protected StoreFmt.Encoder encoder;
    protected StoreFmt.Parser parser;


    /**
     * Use a higher-level parsing tech to parse string
     * There has to be a simpler way to do this
     * Than to stick the object in a quad and parse the quad
     *
     * OTOH this is turns out to be a useful sanity-check of lower level methods
     *
     * @param o object to parse
     * @return parsed object
     */
    private Node parse(String o) {

        var line = String.format("<http://one.com#one> <https://two.com#two> %s .", o);
        var dataSet = RDFParser.create().lang(RDFLanguages.NQUADS).source(new StringReader(line)).toDataset();
        Iterator<Quad> it = dataSet.asDatasetGraph().find();
        var quad = it.next();
        return quad.getObject();
    }

    @Test public void testInts() {
        for (int i : List.of(6,9,128,257,32767,32768,32769,65535,65536,65537,8388608,Integer.MAX_VALUE)) {
            byteBuffer.clear();
            StoreFmt.encodeInt(byteBuffer, i);
            assertThat(StoreFmt.decodeInt(byteBuffer.flip())).isEqualTo(i);
        }
    }

    @Test public void testStrings() {
        encoder.formatStrings(byteBuffer, List.of("value1"));
        var parser = new StoreFmtByString.Parser();
        var result = new ArrayList<String>();
        parser.parseStrings(byteBuffer.flip(), result);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0)).isEqualTo("value1");
    }

    @Test public void testMultipleStrings() {
        encoder.formatStrings(byteBuffer, List.of("value1", "VALU2", "v_a_l_u_3"));
        var result = new ArrayList<String>();
        assertThat(parser.parseStrings(byteBuffer.flip(), result)).isEqualTo(parser);
        assertThat(result.size()).isEqualTo(3);
        assertThat(result.get(0)).isEqualTo("value1");
        assertThat(result.get(1)).isEqualTo("VALU2");
        assertThat(result.get(2)).isEqualTo("v_a_l_u_3");
    }

    @Test public void testNodes() {

        assertThat(encoder.formatSingleNode(byteBuffer, Node.ANY)).isEqualTo(encoder);
        var n1 = parser.parseSingleNode(byteBuffer.flip());
        assertThat(n1).isSameAs(Node.ANY);

        byteBuffer.clear();
        encoder.formatSingleNode(byteBuffer, SSE.parseNode("<https://starwars.com#sector_Arkanis>"));
        var n2 = parser.parseSingleNode(byteBuffer.flip());
        assertThat(n2.isURI()).isTrue();
        assertThat(n2.getURI()).isEqualTo(NodeFactory.createURI("https://starwars.com#sector_Arkanis").getURI());
        assertThat(n2).isEqualTo(parse("<https://starwars.com#sector_Arkanis>"));

        byteBuffer.clear();
        encoder.formatSingleNode(byteBuffer, SSE.parseNode("\"L10\""));
        var n3 = parser.parseSingleNode(byteBuffer.flip());
        assertThat(n3.isLiteral()).isTrue();
        assertThat(n3.getLiteralLexicalForm()).isEqualTo("L10");
        assertThat(n3).isEqualTo(parse("\"L10\""));
    }

    @Test public void testNodesTogether() {

        assertThat(encoder.formatSingleNode(byteBuffer, Node.ANY)).isEqualTo(encoder);

        encoder.formatSingleNode(byteBuffer, SSE.parseNode("<https://starwars.com#sector_Arkanis>"));

        encoder.formatSingleNode(byteBuffer, SSE.parseNode("\"L10\""));

        var n1 = parser.parseSingleNode(byteBuffer.flip());
        assertThat(n1).isSameAs(Node.ANY);

        var n2 = parser.parseSingleNode(byteBuffer);
        assertThat(n2.isURI()).isTrue();
        assertThat(n2.getURI()).isEqualTo(NodeFactory.createURI("https://starwars.com#sector_Arkanis").getURI());
        assertThat(n2).isEqualTo(parse("<https://starwars.com#sector_Arkanis>"));

        var n3 = parser.parseSingleNode(byteBuffer);
        assertThat(n3.isLiteral()).isTrue();
        assertThat(n3.getLiteralLexicalForm()).isEqualTo("L10");
        assertThat(n3).isEqualTo(parse("\"L10\""));
    }

    /**
     * TODO (AP) confirming to myself how serialized forms of Nodes work
     */
    @Test public void testNodesUsingFactory() {

        // Literal
        var literal = NodeFactory.createLiteralString("R16");
        assertThat(literal.isConcrete()).isTrue();
        assertThat(literal.isLiteral()).isTrue();
        assertThat(literal.isURI()).isFalse();
        assertThat(literal instanceof Node_Literal).isTrue();
        assertThat(literal.getLiteralLexicalForm()).isEqualTo("R16");
        assertThat(literal).isEqualTo(parse("\"R16\""));

        encoder.formatSingleNode(byteBuffer, literal);
        var result = parser.parseSingleNode(byteBuffer.flip());
        assertThat(result.getLiteralLexicalForm()).isEqualTo("R16");
        assertThat(result).isEqualTo(parse("\"R16\""));

        // URI
        var nuri = NodeFactory.createURI("https://starwars.com#sector_Arkanis");
        assertThat(nuri.isURI()).isTrue();
        assertThat(nuri.isConcrete()).isTrue();
        assertThat(nuri.isLiteral()).isFalse();
        assertThat(nuri instanceof Node_URI).isTrue();
        assertThat(nuri).isEqualTo(parse("<https://starwars.com#sector_Arkanis>"));

        byteBuffer.clear();

        encoder.formatSingleNode(byteBuffer, nuri);
        result = parser.parseSingleNode(byteBuffer.flip());
        assertThat(result.getURI()).isEqualTo("https://starwars.com#sector_Arkanis");
        assertThat(result).isEqualTo(parse("<https://starwars.com#sector_Arkanis>"));

        byteBuffer.clear();

        // Blank
        var blank = NodeFactory.createBlankNode("blanque");
        assertThat(blank.isBlank()).isTrue();
        assertThat(blank.isURI()).isFalse();
        assertThat(blank.isConcrete()).isTrue();
        assertThat(blank.isLiteral()).isFalse();
        assertThat(blank instanceof Node_Blank).isTrue();

        encoder.formatSingleNode(byteBuffer, blank);
        result = parser.parseSingleNode(byteBuffer.flip());
        assertThat(result.getBlankNodeLabel()).isEqualTo("blanque");

        byteBuffer.clear();

        var any = Node.ANY;
        assertThat(any.isBlank()).isFalse();
        assertThat(any.isLiteral()).isFalse();
        assertThat(any.isConcrete()).isFalse();
        assertThat(any.isURI()).isFalse();
        assertThat(any).isEqualTo(Node.ANY);

        encoder.formatSingleNode(byteBuffer, any);
        result = parser.parseSingleNode(byteBuffer.flip());
        assertThat(result).isEqualTo(Node.ANY);
    }

    @Test public void testTriples() {
        encoder.formatTriple(byteBuffer, SSE.parseNode(":s"), SSE.parseNode(":p"), SSE.parseNode(":o"));
        var result = new ArrayList<Node>();
        assertThat(parser.parseTriple(byteBuffer.flip(), result)).isEqualTo(parser);
        assertThat(result.size()).isEqualTo(3);
        assertThat(result.get(0)).isEqualTo(SSE.parseNode(":s"));
        assertThat(result.get(1)).isEqualTo(SSE.parseNode(":p"));
        assertThat(result.get(2)).isEqualTo(SSE.parseNode(":o"));
    }

    @Test public void testTripleOfStuff() {

        var l1 = NodeFactory.createLiteralString("Literal_the_first");
        var b2 = NodeFactory.createBlankNode();
        var b2Id = b2.getBlankNodeLabel();
        var u3 = NodeFactory.createURI("https://starwars.com#grid_M13");
        encoder.formatTriple(byteBuffer, l1,b2,u3);
        var result = new ArrayList<Node>();
        assertThat(parser.parseTriple(byteBuffer.flip(), result)).isEqualTo(parser);
        assertThat(result.size()).isEqualTo(3);
        var l1_ = result.get(0);
        assertThat(l1_).isEqualTo(NodeFactory.createLiteralString("Literal_the_first"));
        var b2_ = result.get(1);
        assertThat(b2_.isBlank()).isTrue();
        assertThat(b2_.getBlankNodeLabel()).isEqualTo(b2Id);
        var u3_ = result.get(2);
        assertThat(u3_.isURI()).isTrue();
        assertThat(u3_).isEqualTo(NodeFactory.createURI("https://starwars.com#grid_M13"));
    }

    @Test public void testLongFormat() {

        var byteBuffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        assertThat(StoreFmt.formatLongVariable(byteBuffer, 127)).isEqualTo(StoreFmt.IntBytes.ONE_BYTE);
        assertThat(StoreFmt.formatLongVariable(byteBuffer, -9)).isEqualTo(StoreFmt.IntBytes.ONE_BYTE);
        assertThat(StoreFmt.formatLongVariable(byteBuffer, -128)).isEqualTo(StoreFmt.IntBytes.ONE_BYTE);
        byteBuffer.flip();
        assertThat(StoreFmt.parseLongVariable(byteBuffer, StoreFmt.IntBytes.ONE_BYTE)).isEqualTo(127);
        assertThat(StoreFmt.parseLongVariable(byteBuffer, StoreFmt.IntBytes.ONE_BYTE)).isEqualTo(-9);
        assertThat(StoreFmt.parseLongVariable(byteBuffer, StoreFmt.IntBytes.ONE_BYTE)).isEqualTo(-128);

        byteBuffer.clear();
        assertThat(StoreFmt.formatLongVariable(byteBuffer, 32767)).isEqualTo(StoreFmt.IntBytes.TWO_BYTES);
        assertThat(StoreFmt.formatLongVariable(byteBuffer, -32768)).isEqualTo(StoreFmt.IntBytes.TWO_BYTES);
        assertThat(StoreFmt.formatLongVariable(byteBuffer, 32768)).isEqualTo(StoreFmt.IntBytes.FOUR_BYTES);
        assertThat(StoreFmt.formatLongVariable(byteBuffer, -32769)).isEqualTo(StoreFmt.IntBytes.FOUR_BYTES);
        byteBuffer.flip();
        assertThat(StoreFmt.parseLongVariable(byteBuffer, StoreFmt.IntBytes.TWO_BYTES)).isEqualTo(32767);
        assertThat(StoreFmt.parseLongVariable(byteBuffer, StoreFmt.IntBytes.TWO_BYTES)).isEqualTo(-32768);
        assertThat(StoreFmt.parseLongVariable(byteBuffer, StoreFmt.IntBytes.FOUR_BYTES)).isEqualTo(32768);
        assertThat(StoreFmt.parseLongVariable(byteBuffer, StoreFmt.IntBytes.FOUR_BYTES)).isEqualTo(-32769);

        byteBuffer.clear();
        assertThat(StoreFmt.formatLongVariable(byteBuffer, Integer.MAX_VALUE)).isEqualTo(StoreFmt.IntBytes.FOUR_BYTES);
        assertThat(StoreFmt.formatLongVariable(byteBuffer, Integer.MIN_VALUE)).isEqualTo(StoreFmt.IntBytes.FOUR_BYTES);
        assertThat(StoreFmt.formatLongVariable(byteBuffer, 1L + Integer.MAX_VALUE)).isEqualTo(StoreFmt.IntBytes.EIGHT_BYTES);
        assertThat(StoreFmt.formatLongVariable(byteBuffer, -1L + Integer.MIN_VALUE)).isEqualTo(StoreFmt.IntBytes.EIGHT_BYTES);
        byteBuffer.flip();
        assertThat(StoreFmt.parseLongVariable(byteBuffer, StoreFmt.IntBytes.FOUR_BYTES)).isEqualTo(Integer.MAX_VALUE);
        assertThat(StoreFmt.parseLongVariable(byteBuffer, StoreFmt.IntBytes.FOUR_BYTES)).isEqualTo(Integer.MIN_VALUE);
        assertThat(StoreFmt.parseLongVariable(byteBuffer, StoreFmt.IntBytes.EIGHT_BYTES)).isEqualTo(1L + Integer.MAX_VALUE);
        assertThat(StoreFmt.parseLongVariable(byteBuffer, StoreFmt.IntBytes.EIGHT_BYTES)).isEqualTo(Integer.MIN_VALUE - 1L);
    }

    /**
     * Utility method for asserting class details within package protection - By String
     * @param store Rocks DB Store
     * @param expectedMode Label Mode (Overwrite or Merge)
     */
    public static void assertRocksDBByString(LabelsStore store, LabelsStoreRocksDB.LabelMode expectedMode) {
        assertInstanceOf(LabelsStoreRocksDB.class, store);
        if (store instanceof LabelsStoreRocksDB rocksDB) {
            assertInstanceOf(StoreFmtByString.Encoder.class, rocksDB.encoder);
            assertInstanceOf(StoreFmtByString.Parser.class, rocksDB.parser);
            assertEquals(expectedMode, rocksDB.labelMode);
        }
    }

    /**
     * Utility method for asserting class details within package protection - By Hash
     * @param store Rocks DB Store
     * @param expectedHasher Hashing function in use
     */
    public static void assertRocksDBByHash(LabelsStore store, Hasher expectedHasher) {
        assertInstanceOf(LabelsStoreRocksDB.class, store);
        if (store instanceof LabelsStoreRocksDB rocksDB) {
            assertInstanceOf(StoreFmtByHash.HashEncoder.class, rocksDB.encoder);
            if (rocksDB.encoder instanceof StoreFmtByHash.HashEncoder hashEncoder) {
                assertInstanceOf(expectedHasher.getClass(), hashEncoder.hasher);
            }
            assertInstanceOf(StoreFmtByHash.OnlyStringParser.class, rocksDB.parser);
        }

    }

}
