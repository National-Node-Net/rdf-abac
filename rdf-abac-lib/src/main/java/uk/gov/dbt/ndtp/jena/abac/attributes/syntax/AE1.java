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

import uk.gov.dbt.ndtp.jena.abac.attributes.ValueTerm;
import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeExpr;
import uk.gov.dbt.ndtp.jena.abac.lib.CxtABAC;
import org.apache.jena.atlas.io.IndentedWriter;

public abstract class AE1 implements AttributeExpr {

    protected final AttributeExpr attrExpr;

    protected AE1(AttributeExpr attrExpr) {
        this.attrExpr = attrExpr;
    }

    @Override
    public ValueTerm eval(CxtABAC cxt) {
        ValueTerm subValue = attrExpr.eval(cxt);
        ValueTerm value = eval(subValue, cxt) ;
        return value;
    }

    protected abstract ValueTerm eval(ValueTerm subValue, CxtABAC cxt);

    protected abstract String sym();

    public AttributeExpr get() { return attrExpr; }

    @Override
    public void print(IndentedWriter out) {
        out.write(sym());
        out.write(" ");
        attrExpr.print(out);
    }

    @Override
    public String toString() {
        return "("+sym()+" "+attrExpr+")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(attrExpr);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        AE1 other = (AE1)obj;
        return Objects.equals(attrExpr, other.attrExpr);
    }
}
