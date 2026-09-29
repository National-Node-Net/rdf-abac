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


package uk.gov.dbt.ndtp.jena.abac;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import uk.gov.dbt.ndtp.jena.abac.attributes.Attribute;
import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeValue;
import uk.gov.dbt.ndtp.jena.abac.attributes.ValueTerm;
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.tokens.Words;
import org.apache.commons.collections4.MultiMapUtils;
import org.apache.commons.collections4.MultiValuedMap;

/**
 * {@code AttributeValueSet} is request-side capabilities that are matched against attribute expressions (the data labelling).
 */
public class AttributeValueSet {

    public static AttributeValueSet EMPTY = new AttributeValueSet(List.of());

    // Attribute to a set of values.
    private final MultiValuedMap<Attribute, ValueTerm> attributes = MultiMapUtils.newSetValuedHashMap();

    private AttributeValueSet(Collection<AttributeValue>aValues) {
        aValues.forEach(av->this.attributes.put(av.attribute(), av.value()));
    }

    public static AttributeValueSet of(AttributeValue attrValue) {
        return new AttributeValueSet(List.of(attrValue));
    }

    public static AttributeValueSet of(Collection<AttributeValue> attrValues) {
        return new AttributeValueSet(attrValues);
    }

    // For test convenience
    public static AttributeValueSet of(String ... attributes) {
        List<AttributeValue> attrValues = new ArrayList<>(attributes.length);
        for ( String a : attributes ) {
            attrValues.add(AttributeValue.of(Attribute.create(a), ValueTerm.TRUE));
        }
        return new AttributeValueSet(attrValues);
    }

    public Collection<Attribute> attributes() { return Collections.unmodifiableCollection(attributes.keySet()); }

    public void attributeValues(Consumer<AttributeValue> action) {
        forEach(attributes, (attr, valueTerm) -> action.accept(AttributeValue.of(attr, valueTerm)));
    }

    public boolean hasAttribute(Attribute attr) {
        return attributes.containsKey(attr);
    }

    public boolean isEmpty() {
        return attributes.isEmpty();
    }

    public Collection<ValueTerm> get(Attribute attribute) {
        return attributes.get(attribute);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ");
        forEach(attributes, (a,v)->sj.add(a.name()+"="+v.asString()));
        return "AttributeValueSet["+sj.toString()+"]";
    }

    public String asString() {
        StringJoiner sj = new StringJoiner(", ");
        forEach(attributes, (a,v)->sj.add(Words.wordStr(a.name())+"="+v.asString()));
        return sj.toString();
    }

    private static void forEach(MultiValuedMap<Attribute, ValueTerm> multiMap, BiConsumer<Attribute, ValueTerm> action) {
        multiMap.mapIterator().forEachRemaining(attr->{
            multiMap.get(attr).forEach(valueTerm->
                action.accept(attr, valueTerm));
        });
    }


    @Override
    public int hashCode() {
        return Objects.hash(attributes);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        AttributeValueSet other = (AttributeValueSet)obj;
        return Objects.equals(attributes, other.attributes);
    }
}
