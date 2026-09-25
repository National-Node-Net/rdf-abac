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

package uk.gov.dbt.ndtp.jena.abac.attributes.syntax;

import uk.gov.dbt.ndtp.jena.abac.attributes.Attribute;
import uk.gov.dbt.ndtp.jena.abac.attributes.Operator;
import uk.gov.dbt.ndtp.jena.abac.attributes.ValueTerm;
import org.junit.jupiter.api.Test;
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.AE_AttrValue;
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.AE_Attribute;
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.AE_RelAny;

import static org.junit.jupiter.api.Assertions.*;

public class TestAE_RelAny {

    @Test
    public void test_sym() {
        AE_RelAny relAny = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertEquals("<", relAny.sym());
    }

    @Test
    public void test_attribute() {
        AE_RelAny relAny = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertEquals(new Attribute("a"),relAny.attribute());
    }

    @Test
    public void test_attribute_value() {
        AE_RelAny relAny = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertEquals(ValueTerm.value("1"),relAny.value());
    }

    @Test
    public void test_to_string() {
        AE_RelAny relAny = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertEquals("(< a 1)",relAny.toString());
    }

    @Test
    public void test_hash_code() {
        AE_RelAny relAny = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertEquals(relAny.hashCode(),relAny.hashCode());
    }

    @Test
    public void test_equals_same() {
        AE_RelAny relAny = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertTrue(relAny.equals(relAny)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_null() {
        AE_RelAny relAny = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertFalse(relAny.equals(null)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_different_class() {
        AE_RelAny relAny = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertFalse(relAny.equals("a")); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_identical() {
        AE_RelAny relAny1 = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        AE_RelAny relAny2 = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertTrue(relAny1.equals(relAny2)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_different_01() {
        AE_RelAny relAny1 = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        AE_RelAny relAny2 = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("2"));
        assertFalse(relAny1.equals(relAny2)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_different_02() {
        AE_RelAny relAny1 = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        AE_RelAny relAny2 = new AE_RelAny(Operator.LT, AE_Attribute.create("b"), AE_AttrValue.create("1"));
        assertFalse(relAny1.equals(relAny2)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_different_03() {
        AE_RelAny relAny1 = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        AE_RelAny relAny2 = new AE_RelAny(Operator.GT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertFalse(relAny1.equals(relAny2)); // we are specifically testing the equals method here
    }
}
