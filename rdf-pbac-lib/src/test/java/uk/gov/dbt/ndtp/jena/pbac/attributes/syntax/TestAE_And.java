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

import org.apache.jena.atlas.io.IndentedWriter;
import org.junit.jupiter.api.Test;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AE_Allow;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AE_And;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AE_Deny;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AE_Var;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TestAE_And {

    @Test
    public void test_sym() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertEquals("&&",aeAnd.sym());
    }

    @Test
    public void test_to_string() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertEquals("(&& * {a})", aeAnd.toString());
    }

    @Test
    public void test_hash_code() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertEquals(aeAnd.hashCode(),aeAnd.hashCode());
    }

    @Test
    public void test_equals_true() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertTrue(aeAnd.equals(new AE_And(AE_Allow.value(), new AE_Var("a")))); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_false_01() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertFalse(aeAnd.equals(new AE_And(AE_Allow.value(), new AE_Var("b")))); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_false_02() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertFalse(aeAnd.equals(new AE_And(AE_Deny.value(), new AE_Var("a")))); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_false_null() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertFalse(aeAnd.equals(null)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_false_class() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertFalse(aeAnd.equals("a")); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_true_same() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertTrue(aeAnd.equals(aeAnd)); // we are specifically testing the equals method here
    }

    @Test
    public void test_print() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        IndentedWriter mockWriter = mock(IndentedWriter.class);
        aeAnd.print(mockWriter);
        verify(mockWriter, times(4)).write(anyString());
    }

}
