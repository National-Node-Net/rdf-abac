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


package uk.gov.dbt.ndtp.jena.abac;

import uk.gov.dbt.ndtp.jena.abac.attributes.Attribute;
import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeSyntaxError;
import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeValue;
import uk.gov.dbt.ndtp.jena.abac.attributes.ValueTerm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestAttributeValue extends AbstractParser {

    @Test public void parse_attrValue_01() { parseAttrValue("k=v"); }
    @Test public void parse_attrValue_02() { parseAttrValue("a"); }

    @Test public void parse_attrValue_03() { parseAttrValue(" k =   v         "); }
    @Test public void parse_attrValue_04() { parseAttrValue("  a  "); }

    @Test public void parse_attrValue_05() { parseAttrValue("'my attr' = 'some value'"); }


    @Test public void parse_bad_attrValue_01() { parseBadAttrValue("k="); }
    @Test public void parse_bad_attrValue_02() { parseBadAttrValue("=v"); }
    @Test public void parse_bad_attrValue_03() { parseBadAttrValue("k<v"); }
    @Test public void parse_bad_attrValue_04() { parseBadAttrValue("k k<v"); }
    @Test public void parse_bad_attrValue_05() { parseBadAttrValue("k < v w"); }
    @Test public void parse_bad_attrValue_06() { parseBadAttrValue("*"); }
    @Test public void parse_bad_attrValue_07() { parseBadAttrValue("!"); }

    @Test public void parse_legacy_attrValue_01() { legacy(false, ()->parseBadAttrValue("user@host")); }
    @Test public void parse_no_legacy_attrValue_01() { legacy(true, ()->parseAttrValue("user@host")); }

    @Test public void parse_legacy_attrValue_02() { legacy(false, ()->parseBadAttrValue("first+last@host")); }
    @Test public void parse_no_legacy_attrValue_02() { legacy(true, ()->parseAttrValue("first+last@host")); }

    @Test
    public void testEquals01() {
        AttributeValue av1 = AE.parseAttrValue("k=v");
        AttributeValue av2 = AE.parseAttrValue("k = v");
        assertTrue(av1.equals(av2));
    }

    @Test
    public void testEquals02() {
        AttributeValue av1 = AE.parseAttrValue("k=v");
        assertTrue(av1.equals(av1));
    }

    @Test
    public void testEquals03() {
        AttributeValue av1 = AE.parseAttrValue("k=v");
        Object av2 = ValueTerm.value(true);
        assertFalse(av1.equals(av2));
    }

    @Test
    public void testEquals04() {
        AttributeValue av1 = AE.parseAttrValue("'my attr' = 'some value'");
        AttributeValue av2 = AE.parseAttrValue("k=v");
        assertFalse(av1.equals(av2));
    }

    @Test
    public void testEquals05() {
        AttributeValue av1 = AE.parseAttrValue("k=v");
        assertFalse(av1.equals(null));
    }

    @Test
    public void testHashCode01() {
        AttributeValue av1 = AE.parseAttrValue("k=v");
        AttributeValue av2 = AE.parseAttrValue("k=v");
        assertEquals(av1.hashCode(), av2.hashCode());
    }

    @Test
    public void testHashCode02() {
        AttributeValue av1 = AE.parseAttrValue("k=v");
        AttributeValue av2 = AE.parseAttrValue("k");
        assertNotEquals(av1.hashCode(), av2.hashCode());
    }

    @Test
    public void testToString01() {
        AttributeValue av1 = AE.parseAttrValue("k=v");
        assertEquals("k=v", av1.toString());
    }

    @Test
    public void testAsString01() {
        AttributeValue av1 = AE.parseAttrValue("k=v");
        assertEquals("k=v", av1.asString());
    }

    @Test
    public void testAsString02() {
        AttributeValue av1 = AE.parseAttrValue("k = v");
        assertEquals("k=v", av1.asString());
    }

    //TODO
    // test passes when ran in isolation, but when the whole suite is ran the actual value is "kv=true"
    @Test
    public void testAsString03() {
        AttributeValue av1 = AE.parseAttrValue("kv");
        assertEquals("kv", av1.asString());
    }

    @Test
    public void testOf01() {
        AttributeValue av1 = AttributeValue.of("k=v", ValueTerm.value(true));
        AttributeValue av2 = AttributeValue.of(Attribute.create("k=v"), ValueTerm.value(true));
        assertTrue(av1.equals(av2));
    }

    @Test
    public void testOf02() {
        AttributeValue av1 = AttributeValue.of("k=v", ValueTerm.value(true));
        AttributeValue av2 = AttributeValue.of(Attribute.create("k"), ValueTerm.value(true));
        assertFalse(av1.equals(av2));
    }

    @Test
    public void testOf03() {
        AttributeValue av1 = AttributeValue.of("k=v", ValueTerm.value(true));
        AttributeValue av2 = AttributeValue.of(Attribute.create("k=v"), ValueTerm.value(false));
        assertFalse(av1.equals(av2));
    }

    private void legacy(boolean setting, Runnable action) {
        boolean b = ABAC.LEGACY;
        ABAC.LEGACY = setting;
        try { action.run(); }
        finally { ABAC.LEGACY = b; }
    }

    private void parseAttrValue(String str) {
        AttributeValue attrVal = AE.parseAttrValue(str);
        assertNotNull(attrVal);
    }

    private void parseBadAttrValue(String str) {
        assertThrows(AttributeSyntaxError.class, ()->AE.parseAttrValue(str), "Parsed bad attribute-value: "+str);
    }
}
