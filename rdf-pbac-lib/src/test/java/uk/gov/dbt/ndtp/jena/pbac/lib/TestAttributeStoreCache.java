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

package uk.gov.dbt.ndtp.jena.pbac.lib;

import uk.gov.dbt.ndtp.jena.pbac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.pbac.Hierarchy;
import uk.gov.dbt.ndtp.jena.pbac.attributes.Attribute;
import uk.gov.dbt.ndtp.jena.pbac.attributes.AttributeValue;
import uk.gov.dbt.ndtp.jena.pbac.attributes.ValueTerm;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestAttributeStoreCache {

    @Test
    public void test_users_true(){
        AttributesStoreLocal asl = new AttributesStoreLocal();
        asl.put("user1", AttributeValueSet.of(AttributeValue.of("k", ValueTerm.TRUE)));
        AttributeStoreCache cache = new AttributeStoreCache(asl, Duration.ofHours(1),Duration.ofHours(1));
        assertTrue(cache.users().contains("user1"));
    }

    @Test
    public void test_users_false(){
        AttributesStoreLocal asl = new AttributesStoreLocal();
        asl.put("user1", AttributeValueSet.of(AttributeValue.of("k", ValueTerm.TRUE)));
        AttributeStoreCache cache = new AttributeStoreCache(asl, Duration.ofHours(1),Duration.ofHours(1));
        assertFalse(cache.users().contains("user2"));
    }

    @Test
    public void test_has_hierarchy_true() {
        AttributesStoreLocal asl = new AttributesStoreLocal();
        Attribute attr = new Attribute("attr");
        asl.addHierarchy(new Hierarchy(attr, List.of(ValueTerm.TRUE)));
        AttributeStoreCache cache = new AttributeStoreCache(asl, Duration.ofHours(1),Duration.ofHours(1));
        assertTrue(cache.hasHierarchy(attr));
    }

    @Test
    public void test_has_hierarchy_false_01() {
        AttributesStoreLocal asl = new AttributesStoreLocal();
        Attribute someAttr = new Attribute("some");
        Attribute otherAttr = new Attribute("other");
        asl.addHierarchy(new Hierarchy(someAttr, List.of(ValueTerm.TRUE)));
        AttributeStoreCache cache = new AttributeStoreCache(asl, Duration.ofHours(1),Duration.ofHours(1));
        assertFalse(cache.hasHierarchy(otherAttr));
    }

    @Test
    public void test_has_empty_hierarchy() {
        AttributesStoreLocal asl = new AttributesStoreLocal();
        Attribute attr = new Attribute("attr");
        asl.addHierarchy(new Hierarchy(attr, List.of()));
        AttributeStoreCache cache = new AttributeStoreCache(asl, Duration.ofHours(1),Duration.ofHours(1));
        assertFalse(cache.hasHierarchy(attr));
    }

    @Test
    public void test_attributes() {
        AttributesStoreLocal asl = new AttributesStoreLocal();
        AttributeValue av = AttributeValue.of("k",ValueTerm.TRUE);
        AttributeValueSet avs = AttributeValueSet.of(av);
        asl.put("user1", avs);
        AttributeStoreCache cache = new AttributeStoreCache(asl, Duration.ofHours(1),Duration.ofHours(1));
        assertEquals(avs, cache.attributes("user1"));
    }

    @Test
    public void test_get_hierarchy() {
        AttributesStoreLocal asl = new AttributesStoreLocal();
        Attribute attr = new Attribute("attr");
        Hierarchy hierarchy = new Hierarchy(attr, List.of(ValueTerm.TRUE));
        asl.addHierarchy(hierarchy);
        AttributeStoreCache cache = new AttributeStoreCache(asl, Duration.ofHours(1),Duration.ofHours(1));
        assertEquals(hierarchy, cache.getHierarchy(attr));
    }

}
