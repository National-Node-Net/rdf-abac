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

import java.util.Objects;

import uk.gov.dbt.ndtp.jena.pbac.attributes.ValueTerm;
import uk.gov.dbt.ndtp.jena.pbac.attributes.AttributeExpr;
import uk.gov.dbt.ndtp.jena.pbac.lib.CxtPBAC;
import org.apache.jena.atlas.io.IndentedWriter;

/**
 * Two argument logical (boolean valued arguments) expression.
 * <p>
 * "&amp;" and "|".
 */
public abstract class AE2_Logical implements AttributeExpr {

    protected final AttributeExpr left;
    protected final AttributeExpr right;

    protected AE2_Logical(AttributeExpr left, AttributeExpr right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public ValueTerm eval(CxtPBAC cxt) {
        return AttrExprEvaluator.evalAnd(left, right, cxt);
    }

    protected abstract String sym();

    public AttributeExpr left() { return left; }

    public AttributeExpr right() { return right; }

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
        return Objects.hash(left, right);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        AE2_Logical other = (AE2_Logical)obj;
        return Objects.equals(left, other.left) && Objects.equals(right, other.right);
    }
}
