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

import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeExpr;
import uk.gov.dbt.ndtp.jena.abac.attributes.ValueTerm;
import uk.gov.dbt.ndtp.jena.abac.attributes.VisitorAttrExpr;
import uk.gov.dbt.ndtp.jena.abac.lib.CxtABAC;
import org.apache.jena.atlas.io.IndentedWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TestAE1 {

    @Test
    public void test_print() {
        TestExpression testExpression = new TestExpression(AE_Allow.value());
        IndentedWriter mockWriter = mock(IndentedWriter.class);
        testExpression.print(mockWriter);
        verify(mockWriter, times(2)).write(anyString());
    }

    @Test
    public void test_to_string() {
        TestExpression testExpression = new TestExpression(AE_Allow.value());
        assertEquals("( *)", testExpression.toString());
    }

    static class TestExpression extends AE1 {
        protected TestExpression(AttributeExpr attrExpr) {
            super(attrExpr);
        }

        @Override
        protected ValueTerm eval(ValueTerm subValue, CxtABAC cxt) {
            return null;
        }

        @Override
        protected String sym() {
            return "";
        }

        @Override
        public void visitor(VisitorAttrExpr visitor) {
            throw new UnsupportedOperationException();
        }

    }
}
