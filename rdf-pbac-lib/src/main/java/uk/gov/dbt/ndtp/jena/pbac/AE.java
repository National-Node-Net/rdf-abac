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

import static org.apache.commons.lang3.StringUtils.containsAny;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;

import uk.gov.dbt.ndtp.jena.pbac.attributes.AttributeExpr;
import uk.gov.dbt.ndtp.jena.pbac.attributes.AttributeParser;
import uk.gov.dbt.ndtp.jena.pbac.attributes.AttributeSyntaxError;
import uk.gov.dbt.ndtp.jena.pbac.attributes.AttributeValue;
import uk.gov.dbt.ndtp.jena.pbac.attributes.ValueTerm;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AE_Allow;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AE_Deny;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AttrExprEvaluator;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.tokens.Words;
import uk.gov.dbt.ndtp.jena.pbac.lib.CxtPBAC;
import uk.gov.dbt.ndtp.jena.pbac.lib.HierarchyGetter;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.atlas.logging.FmtLog;

/** Public API to attribute parsing, evaluation and serialization. */
public final class AE {

    private AE(){}

    public static final AttributeExpr ALLOW = AE_Allow.value();
    public static final AttributeExpr DENY = AE_Deny.value();

    /**
     * Parse a string to get an {@link AttributeExpr}.
     */
    public static AttributeExpr parseExpr(String string) {
        if ( string == null ) {
            return null;
        }
        return parse(string, AttributeParser::parseAttrExpr);
    }

    /**
     * Parse a string to get an {@link AttributeValue}.
     */
    public static AttributeValue parseAttrValue(String string) {
        if ( string == null ) {
            return null;
        }
        return parse(string, AttributeParser::parseAttrValue);
    }

    /**
     * Parse a string of comma-separated {@link AttributeExpr AttributeExprs}.
     */
    public static List<AttributeExpr> parseExprList(String string) {
        if ( string == null ) {
            return List.of();
        }
        return parse(string, AttributeParser::parseAttrExprList);
    }

    /**
     * Parse a string to get a list of {@link AttributeValue}.
     */
    public static List<AttributeValue> parseAttrValueList(String string) {
        if ( string == null ) {
            return List.of();
        }
        return legacy(string,
                      AttributeParser::parseAttrValueList );
    }

    /**
     * Parse a string to get a list of {@link ValueTerm}
     */
    public static List<ValueTerm> parseValueTermList(String string) {
        return parse(string, AttributeParser::parseValueTermList);
    }

    private static <X> X parse(String string, Function<String, X> action) {
        if ( PBAC.LEGACY ) {
            return legacy(string, action);
        }
        return parseEx(string, action);
    }

    private static <X> X parseEx(String string, Function<String, X> action) {
        try {
            return action.apply(string);
        } catch (AttributeSyntaxError ex) {
            FmtLog.error(PBAC.AttrLOG, "Parse error: %s : Input = |%s|", ex.getMessage(), string);
            throw ex;
        }
    }

    /** Characters not allowed in legacy bare word, no value, attribute value strings. */
    private static final char[] badLegacyChars = { '=', ' ', ',', '(',')', '<','>', '{', '}', '!', '*', '\t', '\n' };

    /**
     * If enabled, try to parse in the current format, but if there is a syntax
     * error, deal with as if it is an old (first PBAC design) attribute - unquoted
     * word.
     * <p>
     * An attribute value that is just a word (no "= value") is already handled
     * by {@link AttributeParserEngine#attributeValue()}.
     */
    private static <X> X legacy(String str, Function<String, X> action) {
        try {
            return parseEx(str, action);
        } catch (AttributeSyntaxError ex) {
            // Test for a few bad characters
            if ( containsAny(str, badLegacyChars) ) {
                throw ex;
            }
            // Add quotes and try again as a quoted string.
            return parseEx(Words.quotedStr(str), action);
        }
    }

    public static ValueTerm attrEval(AttributeExpr expr, CxtPBAC env) {
        return AttrExprEvaluator.attrExprEval(expr, env);
    }

    public static Hierarchy parseHierarchy(String string) {
        return AttributeParser.parseHierarchy(string);
    }

    /**
     *  Serialize an attribute expression list.
     */
    public static String serialize(AttributeExpr aExpr) {
        return aExpr.str();
    }

    /**
     *  Serialize an attribute expression list.
     */
    public static String serialize(List<AttributeExpr> aExprs) {
        StringJoiner sj = new StringJoiner(", ");
        aExprs.forEach(e -> sj.add(serialize(e)));
        return sj.toString();
    }

    /**
     *  Serialize an attribute expression list.
     */
    public static List<String> asStrings(List<AttributeExpr> aExprs) {
        return Iter.iter(aExprs).map(AE::serialize).toList();
    }

    // Convenience evaluation.
    public static ValueTerm eval(String attributeExpr, String attributeValues) {
        return eval(attributeExpr, attributeValues, Hierarchy.noHierarchy);
    }

    public static ValueTerm eval(String attributeExpr, String attributeValues, HierarchyGetter getHierarchy) {
        AttributeExpr attrExpr = AE.parseExpr(attributeExpr);
        List<AttributeValue> values = AE.parseAttrValueList(attributeValues);
        AttributeValueSet avs = AttributeValueSet.of(values);
        return eval(attrExpr, avs, getHierarchy);
    }

    public static ValueTerm eval(AttributeExpr attrExpr, AttributeValueSet avs, HierarchyGetter getHierarchy) {
        if ( getHierarchy == null ) {
            getHierarchy = Hierarchy.noHierarchy;
        }
        CxtPBAC cxt = CxtPBAC.context(avs, getHierarchy, null);
        return AE.attrEval(attrExpr, cxt);
    }
}
