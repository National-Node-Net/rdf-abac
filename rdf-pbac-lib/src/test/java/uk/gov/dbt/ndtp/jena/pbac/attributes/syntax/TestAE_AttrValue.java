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

package uk.gov.dbt.ndtp.jena.pbac.attributes.syntax;

import uk.gov.dbt.ndtp.jena.pbac.attributes.AttributeException;
import org.apache.jena.atlas.io.IndentedWriter;
import org.junit.jupiter.api.Test;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AE_AttrValue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TestAE_AttrValue {

    @Test
    public void test_is_string_true() {
        AE_AttrValue value = AE_AttrValue.create("a");
        assertTrue(value.isString());
    }

    @Test
    public void test_is_string_false() {
        AE_AttrValue value = AE_AttrValue.create("true");
        assertFalse(value.isString());
    }

    @Test
    public void test_get_string() {
        AE_AttrValue value = AE_AttrValue.create("a");
        assertEquals("a", value.getString());
    }

    @Test
    public void test_get_string_exception() {
        AE_AttrValue value = AE_AttrValue.create("true");
        Exception exception = assertThrows(AttributeException.class, value::getString);
        assertEquals("Not a string value", exception.getMessage());
    }

    @Test
    public void test_get_boolean_true() {
        AE_AttrValue value = AE_AttrValue.create("true");
        assertEquals(true, value.getBoolean());
    }

    @Test
    public void test_get_boolean_false() {
        AE_AttrValue value = AE_AttrValue.create("false");
        assertEquals(false, value.getBoolean());
    }

    @Test
    public void test_get_boolean_exception() {
        AE_AttrValue value = AE_AttrValue.create("a");
        Exception exception = assertThrows(AttributeException.class, value::getBoolean);
        assertEquals("Not a boolean value", exception.getMessage());
    }

    @Test
    public void test_to_string() {
        AE_AttrValue value = AE_AttrValue.create("a");
        assertEquals("a", value.toString());
    }

    @Test
    public void test_to_string_boolean_true() {
        AE_AttrValue value = AE_AttrValue.create("true");
        assertEquals("true", value.toString());
    }

    @Test
    public void test_to_string_boolean_false() {
        AE_AttrValue value = AE_AttrValue.create("false");
        assertEquals("false", value.toString());
    }

    @Test
    public void test_print_boolean_true() {
        AE_AttrValue value = AE_AttrValue.create("true");
        IndentedWriter mockWriter = mock(IndentedWriter.class);
        value.print(mockWriter);
        verify(mockWriter).print("true");
    }

    @Test
    public void test_print_boolean_false() {
        AE_AttrValue value = AE_AttrValue.create("false");
        IndentedWriter mockWriter = mock(IndentedWriter.class);
        value.print(mockWriter);
        verify(mockWriter).print("false");
    }

    @Test
    public void test_hash_code() {
        AE_AttrValue value = AE_AttrValue.create("a");
        assertEquals(value.hashCode(),value.hashCode());
    }

    @Test
    public void test_equals_same() {
        AE_AttrValue value = AE_AttrValue.create("a");
        assertTrue(value.equals(value)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_null() {
        AE_AttrValue value = AE_AttrValue.create("a");
        assertFalse(value.equals(null)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_different_class() {
        AE_AttrValue value = AE_AttrValue.create("a");
        assertFalse(value.equals("a")); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_similar_01() {
        AE_AttrValue value1 = AE_AttrValue.create("true");
        AE_AttrValue value2 = AE_AttrValue.create("true");
        assertTrue(value1.equals(value2)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_similar_02() {
        AE_AttrValue value1 = AE_AttrValue.create("a");
        AE_AttrValue value2 = AE_AttrValue.create("a");
        assertTrue(value1.equals(value2)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_different_01() {
        AE_AttrValue value1 = AE_AttrValue.create("a");
        AE_AttrValue value2 = AE_AttrValue.create("b");
        assertFalse(value1.equals(value2)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_different_02() {
        AE_AttrValue value1 = AE_AttrValue.create("true");
        AE_AttrValue value2 = AE_AttrValue.create("false");
        assertFalse(value1.equals(value2)); // we are specifically testing the equals method here
    }
}
