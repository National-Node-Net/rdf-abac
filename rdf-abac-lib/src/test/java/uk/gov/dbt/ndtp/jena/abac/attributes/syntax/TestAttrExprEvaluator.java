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
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.AE_Allow;
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.AE_Deny;
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.AttrExprEvaluator;
import uk.gov.dbt.ndtp.jena.abac.lib.CxtABAC;
import org.apache.jena.atlas.lib.NotImplemented;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestAttrExprEvaluator {

    private final CxtABAC mockContext = mock(CxtABAC.class);
    private final Attribute testAttribute = Attribute.create("test");

    @Test
    public void test_eval_null() {
        when(mockContext.getValue(testAttribute)).thenReturn(null);
        AttrExprEvaluator.eval(Operator.NE, Attribute.create("test"), ValueTerm.TRUE, mockContext);
        assertEquals(ValueTerm.FALSE, AttrExprEvaluator.eval(Operator.NE, Attribute.create("test"), ValueTerm.TRUE, mockContext));
    }

    @Test
    public void test_eval_empty() {
        when(mockContext.getValue(testAttribute)).thenReturn(new ArrayList<>());
        AttrExprEvaluator.eval(Operator.NE, Attribute.create("test"), ValueTerm.TRUE, mockContext);
        assertEquals(ValueTerm.FALSE, AttrExprEvaluator.eval(Operator.NE, Attribute.create("test"), ValueTerm.TRUE, mockContext));
    }

    @Test
    public void test_eval_ne_true() {
        ValueTerm[] array = {ValueTerm.TRUE, ValueTerm.FALSE};
        Collection<ValueTerm> valueTerms = Arrays.asList(array);
        when(mockContext.getValue(testAttribute)).thenReturn(valueTerms);
        assertEquals(ValueTerm.TRUE, AttrExprEvaluator.eval(Operator.NE, Attribute.create("test"), ValueTerm.FALSE, mockContext));
    }

    @Test
    public void test_eval_ne_false() {
        ValueTerm[] array = {ValueTerm.TRUE, ValueTerm.FALSE};
        Collection<ValueTerm> valueTerms = Arrays.asList(array);
        when(mockContext.getValue(testAttribute)).thenReturn(valueTerms);
        assertEquals(ValueTerm.TRUE, AttrExprEvaluator.eval(Operator.NE, Attribute.create("test"), ValueTerm.TRUE, mockContext));
    }

    @Test
    public void test_eval_exception_ge() {
        ValueTerm[] array = {ValueTerm.TRUE, ValueTerm.FALSE};
        Collection<ValueTerm> valueTerms = Arrays.asList(array);
        when(mockContext.getValue(testAttribute)).thenReturn(valueTerms);
        assertThrows(NotImplemented.class, () -> {
            AttrExprEvaluator.eval(Operator.GE, Attribute.create("test"), ValueTerm.TRUE, mockContext);
        });
    }

    @Test
    public void test_eval_exception_gt() {
        ValueTerm[] array = {ValueTerm.TRUE, ValueTerm.FALSE};
        Collection<ValueTerm> valueTerms = Arrays.asList(array);
        when(mockContext.getValue(testAttribute)).thenReturn(valueTerms);
        assertThrows(NotImplemented.class, () -> {
            AttrExprEvaluator.eval(Operator.GT, Attribute.create("test"), ValueTerm.TRUE, mockContext);
        });
    }

    @Test
    public void test_eval_exception_le() {
        ValueTerm[] array = {ValueTerm.TRUE, ValueTerm.FALSE};
        Collection<ValueTerm> valueTerms = Arrays.asList(array);
        when(mockContext.getValue(testAttribute)).thenReturn(valueTerms);
        assertThrows(NotImplemented.class, () -> {
            AttrExprEvaluator.eval(Operator.LE, Attribute.create("test"), ValueTerm.TRUE, mockContext);
        });
    }

    @Test
    public void test_eval_exception_lt() {
        ValueTerm[] array = {ValueTerm.TRUE, ValueTerm.FALSE};
        Collection<ValueTerm> valueTerms = Arrays.asList(array);
        when(mockContext.getValue(testAttribute)).thenReturn(valueTerms);
        assertThrows(NotImplemented.class, () -> {
            AttrExprEvaluator.eval(Operator.LT, Attribute.create("test"), ValueTerm.TRUE, mockContext);
        });
    }

    @Test
    public void test_eval_and_true() {
        assertEquals(ValueTerm.TRUE, AttrExprEvaluator.evalAnd(AE_Allow.value(),AE_Allow.value(),mockContext));
    }

    @Test
    public void test_eval_and_false() {
        assertEquals(ValueTerm.FALSE, AttrExprEvaluator.evalAnd(AE_Deny.value(),AE_Deny.value(),mockContext));
    }

}
