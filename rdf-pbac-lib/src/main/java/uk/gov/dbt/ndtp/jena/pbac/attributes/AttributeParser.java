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


package uk.gov.dbt.ndtp.jena.pbac.attributes;

import java.util.List;
import java.util.function.Function;

import uk.gov.dbt.ndtp.jena.pbac.AE;
import uk.gov.dbt.ndtp.jena.pbac.Hierarchy;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AEX;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AE_RelAny;

/**
 * Attribute parser.
 * The public API is in {@link AE}
 */
public class AttributeParser {

    /** @deprecated Use {@link #parseAttrExpr}    */
    @Deprecated
    public static AttributeExpr parseExpr(String string) {
        return parseAttrExpr(string);
    }

    /** Parse an attribute value expression */
    public static AttributeExpr parseAttrExpr(String string) {
        if ( string.equals(AEX.STR_ALLOW) )
            return AE.ALLOW;
        if ( string.equals(AEX.STR_DENY) )
            return AE.DENY;

        AttributeExpr expr = attributeExpression(string);
        return expr;
    }

    private static AttributeExpr attributeExpression(String string) {
        AttributeParserEngine parser = new AttributeParserEngine(string);
        AttributeExpr expr = parser.attributeExpression();
        checkEndOfInput(parser);
        check(expr);
        return expr;
    }

    private static void check(AttributeExpr expr) {
        WalkerAttrExpr.walk(expr, visitor);
    }

    private static final VisitorAttrExpr visitor = new VisitorAttrExpr() {
        @Override
        public void visit(AE_RelAny element) {
            Operator op = element.relation();
            if ( op != Operator.EQ && op != Operator.NE )
                throw new AttributeSyntaxError("Operator not supported: "+op+" at "+this);
        }
    };

    private static void checkEndOfInput(AttributeParserEngine parser) {
        if ( ! parser.endOfTokens() )
            throw new AttributeSyntaxError("More tokens: "+parser.tokenizer().peek()+" ...");
            //PBAC.AttrExprLOG.warn("More tokens: "+tok.peek()+" ...");
    }

    /** Parse an attribute value expression */
    public static AttributeValue parseAttrValue(String string) {
        AttributeParserEngine parser = new AttributeParserEngine(string);
        AttributeValue value = parser.attributeValue();
        checkEndOfInput(parser);
        return value;
    }

    /** Parse an attribute-value list. Only plain attributes and attribute-value pairs (using "=") are allowed. */
    public static List<AttributeValue> parseAttrValueList(String string) {
        Function<AttributeParserEngine, AttributeValue> parseOneItem = (parser) -> parser.attributeValue();
        List<AttributeValue> x = parseList$(string, parseOneItem);
        return x;
    }

    /** Parse a comma separated list to get a {@code List<AttributeExpr>} */
    public static List<AttributeExpr> parseAttrExprList(String string) {
        if ( string.equals(AEX.STR_ALLOW) )
            return List.of(AE.ALLOW);
        if ( string.equals(AEX.STR_DENY) )
            return List.of(AE.DENY);
        Function<AttributeParserEngine, AttributeExpr> parseOneItem = (parser) -> {
            AttributeExpr expr = parser.attributeExpression();
            check(expr);
            return expr;
        };
        List<AttributeExpr> x = parseList$(string, parseOneItem);
        return x;
    }

    /** Parse a comma separated list of value terms to get a {@code List<ValueTerm>} */
    public static List<ValueTerm> parseValueTermList(String string) {
        AttributeParserEngine parser = new AttributeParserEngine(string);
        List<ValueTerm> x = parser.parseListValues();
        checkEndOfInput(parser);
        return x;
    }

    /** Parse a hierarchy */
    public static Hierarchy parseHierarchy(String string) {
        AttributeParserEngine parser = new AttributeParserEngine(string);
        Hierarchy hierarchy = parser.hierarchy();
        checkEndOfInput(parser);
        return hierarchy;
    }

    // Parse list - pass in a function to parse one term.
    private static <X> List<X> parseList$(String string, Function<AttributeParserEngine, X> parseOneItem) {
        AttributeParserEngine parser = new AttributeParserEngine(string);
        return parser.parseList(parseOneItem);
    }
}
