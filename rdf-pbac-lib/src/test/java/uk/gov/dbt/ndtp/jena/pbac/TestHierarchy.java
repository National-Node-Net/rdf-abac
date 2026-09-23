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

import static uk.gov.dbt.ndtp.jena.pbac.Hierarchy.Comparison.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import uk.gov.dbt.ndtp.jena.pbac.Hierarchy.Comparison;
import uk.gov.dbt.ndtp.jena.pbac.attributes.Attribute;
import uk.gov.dbt.ndtp.jena.pbac.attributes.AttributeException;
import uk.gov.dbt.ndtp.jena.pbac.attributes.ValueTerm;
import uk.gov.dbt.ndtp.jena.pbac.lib.HierarchyGetter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class TestHierarchy {

    @Test public void hierarchy_00() {
        Hierarchy h0 = Hierarchy.create("HIER");
        Assertions.assertNotNull(h0);
    }

    @Test public void hierarchy_01() {
        assertThrows(IllegalArgumentException.class, () -> Hierarchy.create("HIER:"));
    }

    @Test public void hierarchy_02() {
        Hierarchy h = Hierarchy.create("HIER", "A");
        checkHierarchy(h);
    }

    @Test public void hierarchy_03() {
        Hierarchy h = Hierarchy.create("HIER", "A", "B", "C");
        checkHierarchy(h);
    }

    @Test public void hierarchy_parse_00() {
        assertThrows(AttributeException.class, () -> Hierarchy.fromString("HIER") ) ;
    }

    @Test public void hierarchy_parse_01() {
        Hierarchy h = Hierarchy.fromString("HIER:");
        Assertions.assertNotNull(h);
    }

    @Test public void hierarchy_parse_02() {
        Hierarchy h = Hierarchy.fromString("HIER: A");
        checkHierarchy(h);
    }

    @Test public void hierarchy_parse_03() {
        Hierarchy h = Hierarchy.fromString("HIER: A, \"With space\", \"With,comma\", C");
        checkHierarchy(h);
    }

    private void checkHierarchy(Hierarchy hierarchy) {
        String x = hierarchy.asString();
        Hierarchy hierarchy2 = Hierarchy.fromString(x);
        assertEquals(hierarchy, hierarchy2);
    }

    ValueTerm av0 = ValueTerm.value("A");
    ValueTerm av1 = ValueTerm.value("B");
    ValueTerm av2 = ValueTerm.value("C");
    ValueTerm av3 = ValueTerm.value("D");
    Hierarchy h0 = Hierarchy.fromString("Test: A, B, C");

    @Test public void  hierarchy_compare_01() {
        compare(EQ, h0, av0, av0);
    }

    @Test public void  hierarchy_compare_02() {
        compare(EQ, h0, av1, av1);
    }

    @Test public void  hierarchy_compare_03() {
        compare(EQ, h0, av2, av2);
    }

    @Test public void  hierarchy_compare_04() {
        compare(LT, h0, av0, av1);
        compare(GT, h0, av1, av0);
    }

    @Test public void  hierarchy_compare_05() {
        compare(LT, h0, av0, av2);
        compare(GT, h0, av2, av0);
    }

    @Test public void  hierarchy_compare_06() {
        compare(LT, h0, av1, av2);
        compare(GT, h0, av2, av1);
    }

    @Test public void  hierarchy_compare_10() {
        ValueTerm avx = ValueTerm.value("X");
        compare(NONE, h0, avx, av2);
        compare(NONE, h0, av2, avx);
    }

    @Test public void  hierarchy_compare_11() {
        ValueTerm avx = ValueTerm.value("X");
        ValueTerm avy = ValueTerm.value("Y");
        compare(NONE, h0, avx, avy);
    }

    @Test public void  hierarchy_compare_12() {
        compare(NONE, h0, av3, av3);
    }

    @Test
    public void hierarchy_constructor_exception_empty_name() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Hierarchy(new Attribute(""), Arrays.asList(ValueTerm.value("A"), ValueTerm.value("B")));
        });
        String expectedMessage = "Hierarchy name is empty";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void hierarchy_constructor_exception_contains_space() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Hierarchy(new Attribute("HI ER"), Arrays.asList(ValueTerm.value("A"), ValueTerm.value("B")));
        });
        String expectedMessage = "Hierarchy name must not contain spaces";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void hierarchy_constructor_exception_contains_nulls() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->{
            new Hierarchy(new Attribute("HIER"), Arrays.asList(null, null));
        });
        String expectedMessage = "Null in attribute value hierarchy";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void hierarchy_constructor_exception_contains_duplicates() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->{
            new Hierarchy(new Attribute("HIER"), Arrays.asList(ValueTerm.value("A"), ValueTerm.value("A")));
        });
        String expectedMessage = "Duplicate in attribute value hierarchy";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void hierarchy_create_exception_01() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Hierarchy.create("HIER", "A", null);
        });
        String expectedMessage = "Null in attribute value hierarchy";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void hierarchy_create_exception_02() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Hierarchy.create(new Attribute("HIER"), Arrays.asList(new String[]{"A", null}));
        });
        String expectedMessage = "Null in attribute value hierarchy";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void hierarchy_hash_code() {
        Hierarchy h1 = Hierarchy.create("HIER", "A");
        assertEquals(3439811, h1.hashCode());
    }

    @Test
    public void hierarchy_equals_true() {
        Hierarchy h1 = Hierarchy.fromString("Test: A, B, C");
        assertTrue(h0.equals(h1));
    }

    @Test
    public void hierarchy_equals_false_01() {
        Hierarchy h1 = Hierarchy.fromString("Test: C, B, A");
        assertFalse(h0.equals(h1));
    }

    @Test
    public void hierarchy_equals_false_02() {
        Hierarchy h1 = Hierarchy.fromString("Other: A, B, C");
        assertFalse(h0.equals(h1));
    }

    @Test
    public void hierarchy_equals_true_as_same() {
        assertTrue(h0.equals(h0)); // we are specifically testing the equals method here
    }

    @Test
    public void hierarchy_equals_false_as_null() {
        assertFalse(h0.equals(null)); // we are specifically testing the equals method here
    }

    @Test
    public void hierarchy_equals_false_as_different_class() {
        String test = "test";
        assertFalse(h0.equals(test)); // we are specifically testing the equals method here
    }

    @Test
    public void hierarchy_no_hierarchy() {
        HierarchyGetter h1 = Hierarchy.noHierarchy;
        assertNull(h1.getHierarchy(Attribute.create("test")));
    }

    private static void compare(Comparison expected, Hierarchy h, ValueTerm av1, ValueTerm av2) {
        Comparison actual = h.compareTo(av1, av2);
        assertEquals(expected, actual);
        //System.out.printf("%s :: compare(%s, %s) -> %s\n", h, av1, av2, actual);
    }
}
