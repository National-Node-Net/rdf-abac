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

package uk.gov.dbt.ndtp.jena.abac.attributes;

import uk.gov.dbt.ndtp.jena.abac.Hierarchy;
import org.junit.jupiter.api.Test;
import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeExpr;
import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeParser;
import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeParserEngine;
import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeSyntaxError;
import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestAttributeParserEngine {

    @Test
    public void testAttributeValue01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeValue av1 = AttributeParser.parseAttrValue("");
        });
        assertEquals("END", exception.getMessage());
    }

    @Test
    public void testHierarchy01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            Hierarchy h1 = AttributeParser.parseHierarchy("");
        });
        assertEquals("END", exception.getMessage());
    }

    @Test
    public void testHierarchy02() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("status public, confidential, sensitive, private");
            Hierarchy h1 = aep.hierarchy();
        });
        assertEquals("Expected ':' after attribute name in hierarchy: [WORD:public]", exception.getMessage());
    }

    @Test
    public void testReadExprOr01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("END", exception.getMessage());
    }

    @Test
    public void testReadExprAnd01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("(a & b) | (a & b) |");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("END", exception.getMessage());
    }

    @Test
    public void testReadExprUnary01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("(a & b | \"*\"");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("No RPAREN: [LPAREN:(]", exception.getMessage());
    }

    @Test
    public void testReadExprUnary02() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("(a }");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("Expected RPAREN: [RBRACE:}]", exception.getMessage());
    }

    @Test
    public void testReadExprUnary03() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("abc & {");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("No RBRACE: [LBRACE:{]", exception.getMessage());
    }

    @Test
    public void testReadExprUnary04() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("a & { }");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("Expected WORD after: [LBRACE:{]", exception.getMessage());
    }


    @Test
    public void testReadExprUnary05() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("{a & b");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("Expected RBRACE: [AMPERSAND:&]", exception.getMessage());
    }

    @Test
    public void testReadExprUnary06() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("{word");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("No RBRACE: [LBRACE:{]", exception.getMessage());
    }

    @Test
    public void testReadExprUnary07_notRecognised() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("?");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("Not recognized: [QMARK:?]", exception.getMessage());
    }

    @Test
    public void testReadExprRel01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("{a} | ");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("END", exception.getMessage());
    }

    //TODO
    // I can't figure out how to trigger the default case - seems to be covered by other exceptions"
//    @Test
//    public void testReadExprRel03() {
//        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
//            AttributeParserEngine aep = new AttributeParserEngine("a ^ b == c");
//            AttributeExpr ae1 = aep.attributeExpression();
//        });
//        assertEquals("Not a relationship operator: '", exception.getMessage());
//    }

    @Test
    public void testReadAttribute01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("1.0:");
            AttributeValue av1 = aep.attributeValue();
        });
        assertEquals("Expected an attribute: Got a number: [DECIMAL:1.0]", exception.getMessage());
    }

    @Test
    public void testReadAttributeValue01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("a & b = {");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("Expected an attribute value: Not recognized: [LBRACE:{]", exception.getMessage());
    }

}
