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

import uk.gov.dbt.ndtp.jena.pbac.attributes.Attribute;
import uk.gov.dbt.ndtp.jena.pbac.attributes.ValueTerm;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AE_Attribute;
import uk.gov.dbt.ndtp.jena.pbac.lib.CxtPBAC;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestAE_Attribute {

    private final CxtPBAC mockContext = mock(CxtPBAC.class);

    @Test
    public void test_name() {
        String name = "test";
        AE_Attribute testAttribute = AE_Attribute.create(name);
        assertEquals(name, testAttribute.name());
    }

    @Test
    public void test_eval() {
        Attribute testAttribute = new Attribute("test");
        when(mockContext.getValue(testAttribute)).thenReturn(null);
        assertEquals(ValueTerm.FALSE, AE_Attribute.eval(testAttribute,mockContext));
    }

    @Test
    public void test_equals_same() {
        AE_Attribute testAttribute = AE_Attribute.create("test");
        assertTrue(testAttribute.equals(testAttribute)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_null() {
        AE_Attribute testAttribute = AE_Attribute.create("test");
        assertFalse(testAttribute.equals(null)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_different_class() {
        AE_Attribute testAttribute = AE_Attribute.create("test");
        assertFalse(testAttribute.equals("test")); // we are specifically testing the equals method here
    }

}
