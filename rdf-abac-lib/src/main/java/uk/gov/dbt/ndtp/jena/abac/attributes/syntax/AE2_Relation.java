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

import java.util.Objects;

import uk.gov.dbt.ndtp.jena.abac.lib.CxtABAC;
import org.apache.jena.atlas.io.IndentedWriter;
import uk.gov.dbt.ndtp.jena.abac.attributes.Attribute;
import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeExpr;
import uk.gov.dbt.ndtp.jena.abac.attributes.Operator;
import uk.gov.dbt.ndtp.jena.abac.attributes.ValueTerm;

/**
 * Two argument relationship.
 * <p>
 * "&lt;", "=", "&gt;"
 * <p>
 * Examples:
 * <pre>
 *    department = engineering
 *    role = manager
 *    status = employee
 *    clearance &gt; secret
 * </pre>
 * Note this is asymmetric. The left-hand side (LHS) is an attribute category,
 * the right-hand side (RHS) is a word which is a constant value.
 */
public abstract class AE2_Relation implements AttributeExpr {

    // Parse tree
    protected final Operator relation;
    protected final AE_Attribute left;
    protected final AE_AttrValue right;

    // Evaluation objects
    private final Attribute attribute;
    private final ValueTerm attrValue;

    protected AE2_Relation(Operator relation, AE_Attribute left, AE_AttrValue right) {
        //left, right AE_Value? (renamed AE_Atom)
        this.relation = relation;
        this.left = left;
        this.right = right;
        // These are "cached" because they get used in eval.
        this.attribute = left.attribute();
        this.attrValue = right.asValue();
    }

    @Override
    public ValueTerm eval(CxtABAC cxt) {
        return AttrExprEvaluator.eval(relation, attribute, attrValue, cxt);
    }

    protected String sym() { return relation.str(); }

    public Operator relation() { return relation; }

    public Attribute attribute() { return attribute; }

    public ValueTerm value() { return attrValue; }

    @Override
    public void print(IndentedWriter out) {
        left.print(out);
        out.write(" ");
        out.write(sym());
        out.write(" ");
        right.print(out);
    }

    @Override
    public String toString() {
        return "("+sym()+" "+left+" "+right+")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(attrValue, attribute, left, relation, right);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        AE2_Relation other = (AE2_Relation)obj;
        return Objects.equals(attrValue, other.attrValue) && Objects.equals(attribute, other.attribute) && Objects.equals(left, other.left)
               && relation == other.relation && Objects.equals(right, other.right);
    }
}
