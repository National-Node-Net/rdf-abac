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

import java.util.Collection;
import java.util.Optional;

import uk.gov.dbt.ndtp.jena.pbac.Hierarchy;
import uk.gov.dbt.ndtp.jena.pbac.Hierarchy.Comparison;
import uk.gov.dbt.ndtp.jena.pbac.attributes.Attribute;
import uk.gov.dbt.ndtp.jena.pbac.attributes.AttributeExpr;
import uk.gov.dbt.ndtp.jena.pbac.attributes.Operator;
import uk.gov.dbt.ndtp.jena.pbac.attributes.ValueTerm;
import uk.gov.dbt.ndtp.jena.pbac.lib.CxtPBAC;
import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.atlas.lib.NotImplemented;

/**
 * Evaluator an attribute expression tree consisting of syntax elements from parsing.
 */
public final class AttrExprEvaluator {

    private AttrExprEvaluator(){}

    /**
     * Evaluate an {@link AttributeExpr} in a given {@link CxtPBAC}.
     */
    public static ValueTerm attrExprEval(AttributeExpr expr, CxtPBAC env) {
        return expr.eval(env);
    }

    /**
     * Examples:
     * <pre>
     *    department = engineering
     *    role = manager
     *    status = employee
     *    clearance = secret
     *       and a hierarchy on clearance of of ordinary &lt; confidential &lt; secret &lt; top-secret
     * </pre>
     * Note this is asymmetric. The left-hand side (LHS) is an attribute category,
     * the right-hand side (RHS) is a word which is a constant value.
     */
    /*package*/ static ValueTerm eval(Operator relation, Attribute attribute, ValueTerm requiredAttrValue, CxtPBAC cxt) {
        Collection<ValueTerm> requestValueTerms = cxt.getValue(attribute);
        if (requestValueTerms == null || requestValueTerms.isEmpty() )
            return ValueTerm.FALSE;

        for ( ValueTerm requestValueTerm : requestValueTerms ) {
            ValueTerm result = switch (relation) {
                case EQ -> {
                    // Calculate for "attribute = value" directly.
                    ValueTerm vt = ValueTerm.value(requestValueTerm.equals(requiredAttrValue));

                    // If false, try for a hierarchy match.
                    if ( vt.equals(ValueTerm.FALSE) ) {
                        Cache<Attribute, Optional<Hierarchy>> cache = cxt.hierarchyCache();
                        // Caches can't hold nulls.
                        Optional<Hierarchy> entry = cache.get(attribute, (a)-> Optional.ofNullable(cxt.getHierarchy(a)) );
                        if ( entry.isEmpty() )
                            // No hierarchy for this attribute.
                            yield vt;
                        Hierarchy attrHierarchy = entry.get();
                        // Try again. Is requiredAttrValue(data) < requestValueTerm(access rights)?
                        Comparison cmp = attrHierarchy.compareTo(requiredAttrValue, requestValueTerm);
                        //System.out.printf("%s :: Data=%s %s Request=%s\n", attrHierarchy, requiredAttrValue, cmp, requestValueTerm);
                        vt = switch (cmp) {
                            case EQ, LT -> ValueTerm.TRUE;
                            default -> ValueTerm.FALSE;
                        };
                    }
                    yield vt;
                }
                case NE -> ValueTerm.value(!requestValueTerm.equals(requiredAttrValue));
                default -> throw new NotImplemented();
            };
            if ( result == ValueTerm.TRUE )
                return result;
        }
        return ValueTerm.FALSE;
    }

    /**
     * Evaluate AND ({@code &}).
     * This short-circuits the right-hand side.
     * If the left-hand side is false, the result is false and the right-hand side is not evaluated.
     */
    /*package*/ static ValueTerm evalAnd(AttributeExpr left, AttributeExpr right, CxtPBAC cxt) {
        boolean bLeft = left.eval(cxt).getBoolean();
        // Done? Or do we need to evaluate the right-hand side?
        if ( ! bLeft ) {
            return ValueTerm.FALSE;
        }
        boolean bRight = right.eval(cxt).getBoolean();
        return ValueTerm.value(bRight);
    }

    /**
     * Evaluate OR ({@code |}).
     * This short-circuits the right-hand side.
     * If the left-hand side is true, the result is true and the right-hand side is not evaluated.
     */
    /*package*/ static ValueTerm evalOr(AttributeExpr left, AttributeExpr right, CxtPBAC cxt) {
        boolean bLeft = left.eval(cxt).getBoolean();
        // Done? Or do we need to evaluate the right-hand side?
        if ( bLeft ) {
            return ValueTerm.TRUE;
        }
        boolean bRight = right.eval(cxt).getBoolean();
        return ValueTerm.value(bRight);
    }
}
