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

package uk.gov.dbt.ndtp.jena.pbac;

import uk.gov.dbt.ndtp.jena.pbac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.pbac.attributes.Attribute;
import uk.gov.dbt.ndtp.jena.pbac.attributes.AttributeValue;
import uk.gov.dbt.ndtp.jena.pbac.attributes.ValueTerm;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class TestAttributeValueSet {

    @Test
    public void testOfTrue() {
        AttributeValue value = AttributeValue.of("some", ValueTerm.value(true));
        AttributeValueSet set = AttributeValueSet.of(value);
        Attribute attr = new Attribute("some");
        assertTrue(set.hasAttribute(attr));
    }

    @Test
    public void testOfFalse() {
        AttributeValue value = AttributeValue.of("some", ValueTerm.value(true));
        AttributeValueSet set = AttributeValueSet.of(value);
        Attribute attr = new Attribute("other");
        assertFalse(set.hasAttribute(attr));
    }

    @Test
    public void testHasAttributeTrue() {
        AttributeValueSet set = AttributeValueSet.of("test");
        Attribute test = new Attribute("test");
        assertTrue(set.hasAttribute(test));
    }

    @Test
    public void testHasAttributeFalse() {
        AttributeValueSet set = AttributeValueSet.of("some");
        Attribute test = new Attribute("other");
        assertFalse(set.hasAttribute(test));
    }

    @Test
    public void testAsString() {
        AttributeValueSet set = AttributeValueSet.of("some");
        assertEquals("some=true", set.asString());
    }

    @Test
    public void testEqualsTrue() {
        AttributeValueSet set1 = AttributeValueSet.of("test");
        AttributeValueSet set2 = AttributeValueSet.of("test");
        assertTrue(set1.equals(set2)); // we are specifically testing the equals method here
    }

    @Test
    public void testEqualsTrueAsSame() {
        AttributeValueSet set1 = AttributeValueSet.of("test");
        assertTrue(set1.equals(set1)); // we are specifically testing the equals method here
    }

    @Test
    public void testEqualsFalse() {
        AttributeValueSet set1 = AttributeValueSet.of("some");
        AttributeValueSet set2 = AttributeValueSet.of("other");
        assertFalse(set1.equals(set2)); // we are specifically testing the equals method here
    }

    @Test
    public void testEqualsFalseAsNull() {
        AttributeValueSet set1 = AttributeValueSet.of("some");
        assertFalse(set1.equals(null)); // we are specifically testing the equals method here
    }

    @Test
    public void testEqualsFalseAsDifferentClass() {
        AttributeValueSet set1 = AttributeValueSet.of("test");
        String test = "test";
        assertFalse(set1.equals(test)); // we are specifically testing the equals method here
    }

    @Test
    public void testHashCode() {
        AttributeValueSet set = AttributeValueSet.of("test");
        assertEquals(3595359,set.hashCode());
    }

    @Test
    public void testIsEmptyTrue() {
        AttributeValueSet set = AttributeValueSet.of();
        assertTrue(set.isEmpty());
    }

    @Test
    public void testIsEmptyFalse() {
        AttributeValueSet set = AttributeValueSet.of("test");
        assertFalse(set.isEmpty());
    }

    @Test
    public void testAttributes() {
        AttributeValueSet set = AttributeValueSet.of("some","other");
        Collection<Attribute> attrs = set.attributes();
        assertEquals(2, attrs.size());
        assertTrue(attrs.contains(new Attribute("some")));
        assertTrue(attrs.contains(new Attribute("other")));
    }

}
