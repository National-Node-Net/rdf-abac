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
import uk.gov.dbt.ndtp.jena.abac.attributes.VisitorAttrExpr;
import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeExpr;
import uk.gov.dbt.ndtp.jena.abac.lib.CxtABAC;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.lib.NotImplemented;

/**
 * Variable that substitutes a context-sensitive value.
 * This is looked up in the execution security context to evaluate.
 */
public class AE_Var implements AttributeExpr {
    private final String varName;

    public AE_Var(String varName) {
        this.varName = varName;
    }

    @Override
    public ValueTerm eval(CxtABAC cxt) {
        throw new NotImplemented();
//        Attribute attr = cxt.getVar(varName);
//        if ( attr == null )
//            return AttrExprValue.FALSE;
//        return AttrExprValue.create(attr.name());
    }

    @Override
    public String toString() {
        return "{"+varName+"}";
    }

    @Override
    public void print(IndentedWriter w) {
        w.write(toString());
    }

    @Override
    public void visitor(VisitorAttrExpr visitor) {visitor.visit(this); }

    @Override
    public int hashCode() {
        return Objects.hash(varName);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        AE_Var other = (AE_Var)obj;
        return Objects.equals(varName, other.varName);
    }
}
