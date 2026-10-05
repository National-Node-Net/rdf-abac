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
import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeException;
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.tokens.Words;
import org.apache.jena.atlas.io.IndentedWriter;

/**
 * Syntax element for a value in an attribute label expression.
 * This is different to {@link ValueTerm} which is a value in the evaluation engine.
 */
public class AE_AttrValue { //implements AttributeExpr {
    private final boolean booleanValue;
    private final String string;
    private final ValueTerm value;

    private static final String TRUE = "true";
    private static final String FALSE = "false";

    public static AE_AttrValue create(String string) {
        Objects.requireNonNull(string);
        if ( TRUE.equalsIgnoreCase(string)) {
            return new AE_AttrValue(true);
        }
        if ( FALSE.equalsIgnoreCase(string)) {
            return new AE_AttrValue(false);
        }
        return new AE_AttrValue(string);
    }

    private AE_AttrValue(boolean val) {
        this.booleanValue = val;
        this.string = null;
        this.value = toValue();
    }

    private AE_AttrValue(String str) {
        this.booleanValue = false;
        this.string = str;
        this.value = toValue();
    }

    public boolean isString() { return string != null; }
    public boolean isBoolean() { return string == null; }

    public String getString() {
        if ( ! isString() ) {
            throw new AttributeException("Not a string value");
        }
        return string;
    }

    public boolean getBoolean() {
        if ( ! isBoolean() ) {
            throw new AttributeException("Not a boolean value");
        }
        return booleanValue;
    }

    public ValueTerm asValue() {
        return value;
    }

    private ValueTerm toValue() {
        if ( isBoolean() ) {
            return ValueTerm.value(getBoolean());
        }
        if ( isString() ) {
            return ValueTerm.value(getString());
        }
        throw new AttributeException("Unset!");
    }

    //@Override
    public void print(IndentedWriter w) {
        if ( isBoolean() ) {
            w.print(booleanValue ? TRUE : FALSE);
        }
        else {
            Words.print(w, string);
        }
    }


    @Override
    public String toString() {
        if ( isBoolean() ) {
            return booleanValue ? TRUE : FALSE;
        }
       return Words.wordStr(string);
    }

    @Override
    public int hashCode() {
        return Objects.hash(booleanValue, string, value);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null ) {
            return false;
        }
        if ( getClass() != obj.getClass() ) {
            return false;
        }
        AE_AttrValue other = (AE_AttrValue)obj;
        return booleanValue == other.booleanValue && Objects.equals(string, other.string) && Objects.equals(value, other.value);
    }
}
