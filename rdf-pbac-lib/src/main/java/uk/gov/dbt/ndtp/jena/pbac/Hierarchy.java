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

import java.util.*;

import uk.gov.dbt.ndtp.jena.pbac.attributes.Attribute;
import uk.gov.dbt.ndtp.jena.pbac.attributes.ValueTerm;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.tokens.Words;
import uk.gov.dbt.ndtp.jena.pbac.lib.HierarchyGetter;
import org.apache.jena.atlas.lib.InternalErrorException;

/**
 * A hierarchy is a controlled set of values for an attribute where a user request
 * with one attribute value implies other values for the same attribute.
 * <p>
 * Hierarchies are written low to high. For example, suppose there is an attribute
 * 'clearance' which has the hierarchy: "ordinary" &lt; "secret" &lt; "top secret".
 * <p>
 * A user request with attribute value 'clearance=secret' will
 * give visibility to data items with 'clearance=ordinary'.
 */

public class Hierarchy {

    /** Hierarchy getter function that always return null (no hierarchy). */
    public static final HierarchyGetter noHierarchy = a->null;

    // Low (index 0) to high
    private final Attribute attribute;
    private final List<ValueTerm> hierarchy;
    private static final String NULL_IN_ATTRIBUTE_VALUE_HIERARCHY = "Null in attribute value hierarchy: ";

    public static Hierarchy create(String attrName, String ... strings) {
        Attribute attr = Attribute.create(attrName);
        return create(attr, strings);
    }

    public static Hierarchy create(Attribute attr, String ... strings) {
        ValueTerm[] a = new ValueTerm[strings.length];
        for (int i = 0 ; i < strings.length ; i ++) {
            if ( strings[i] == null )
                throw new IllegalArgumentException(NULL_IN_ATTRIBUTE_VALUE_HIERARCHY+Arrays.asList(strings));
            a[i] = ValueTerm.value(strings[i]);
        }
        return new Hierarchy(attr, Arrays.asList(a));
    }

    public static Hierarchy create(Attribute attr, List<String> strings) {
        ValueTerm[] a = new ValueTerm[strings.size()];
        for (int i = 0 ; i < strings.size() ; i ++) {
            if ( strings.get(i) == null )
                throw new IllegalArgumentException(NULL_IN_ATTRIBUTE_VALUE_HIERARCHY+ List.of(strings));
            a[i] = ValueTerm.value(strings.get(i));
        }
        return new Hierarchy(attr, Arrays.asList(a));
    }

    public Hierarchy(Attribute attr, List<ValueTerm> values) {
        this.attribute = attr;
        this.hierarchy = values;
        checkName();
        checkNoNulls();
        checkNoDuplicates();
    }

    private void checkName() {
        String name = attribute.name();
        if ( name.isEmpty() )
            throw new IllegalArgumentException("Hierarchy name is empty");
        if ( name.contains(" ") )
            throw new IllegalArgumentException("Hierarchy name must not contain spaces: "+attribute);
        if ( name.contains(":") )
            throw new IllegalArgumentException("Hierarchy name must not contain colon: "+attribute);
    }

    private void checkNoNulls() {
        // ArrayList supports null.
        for (int i = 0; i < hierarchy.size(); i++)
            if (hierarchy.get(i) == null )
                throw new IllegalArgumentException(NULL_IN_ATTRIBUTE_VALUE_HIERARCHY+hierarchy);
    }

    private void checkNoDuplicates() {
        Set<ValueTerm> setOf = new HashSet<>(hierarchy);
        if ( setOf.size() != hierarchy.size() )
            throw new IllegalArgumentException("Duplicate in attribute value hierarchy: "+hierarchy);
    }

    public Attribute attribute() { return attribute; }

    public List<ValueTerm> values() { return hierarchy; }

    /** Format: "name: a,b,c" - syntactically valid comma separated list */
    public String asString() {
        StringJoiner sj = new StringJoiner(", ");
        hierarchy.forEach(valueTerm-> sj.add(valueTerm.asString()) );
        return Words.wordStr(attribute.name())+": "+sj.toString();
    }

    /** From a valid comma separated list */
    public static Hierarchy fromString(String string) {
        return AE.parseHierarchy(string);
    }

    public enum Comparison {
        LT, EQ, GT,
        NONE,
    }
    /**
     * Compare two AttrValues.
     * <p>
     * Cost is linear in the number of attribute values in the hierarchy.
     * <p>
     * Returns: Comparison; v1 CMP v2.
     * Hierarchy list are stored  "low to high"
     * <ul>
     * <li> LT, EQ, GT if both elements are in the hierarchy.
     * <li> NONE if either of v1 and v2 is not in the hierarchy.
     * </ul>
     */
    public Comparison compareTo(ValueTerm v1, ValueTerm v2) {
        Objects.requireNonNull(v1);
        Objects.requireNonNull(v2);
        if ( v1.equals(v2) ) {
            if ( hierarchy.contains(v1) ) {
                return Comparison.EQ;
            }
            return Comparison.NONE;
        }
        int idx1 = -1;
        int idx2 = -1;
        for (int i = 0; i < hierarchy.size(); i++) {
            ValueTerm v = hierarchy.get(i);
            if ( idx1 == -1 && v1.equals(v) ) {
                idx1 = i;
            }
            else if ( idx2 == -1 && v2.equals(v) ) {
                idx2 = i;
            }

            if ( idx1 != -1 && idx2 != -1 ) {
                return getComparison(idx1, idx2);
            }
        }
        return Comparison.NONE;
    }

    private static Comparison getComparison(int idx1, int idx2) {
        if ( idx1 < idx2) {
            return Comparison.LT;
        }
        else if ( idx1 > idx2) {
            return Comparison.GT;
        }
        else {
            // this should never happen
            throw new InternalErrorException();
        }
    }

    @Override
    public String toString() {
        return attribute.name()+": "+hierarchy.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(hierarchy, attribute);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        Hierarchy other = (Hierarchy)obj;
        return Objects.equals(hierarchy, other.hierarchy) && Objects.equals(attribute, other.attribute);
    }
}
