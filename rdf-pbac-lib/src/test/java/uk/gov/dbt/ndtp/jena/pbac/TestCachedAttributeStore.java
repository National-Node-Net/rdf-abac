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

import org.apache.jena.assembler.exceptions.AssemblerException;
import org.junit.jupiter.api.Assertions;
import uk.gov.dbt.ndtp.jena.pbac.attributes.Attribute;
import uk.gov.dbt.ndtp.jena.pbac.lib.AttributeStoreCache;
import uk.gov.dbt.ndtp.jena.pbac.lib.AttributesStore;
import uk.gov.dbt.ndtp.jena.pbac.lib.DatasetGraphPBAC;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TestCachedAttributeStore {

    private static final String DIR = "src/test/files/dataset/";

    private static final AttributeValueSet SAMPLE_ATTRIBUTE_VALUE_SET =
            AttributeValueSet.of("attribute1=value1,attribute2=value2,attribute3=value3");

    private static final Hierarchy SAMPLE_HIERARCHY =
            Hierarchy.create("Test", "attribute1", "attribute2", "attribute3");


    @Test
    public void test_setup_happyPath() {
        DatasetGraphPBAC datasetGraphPBAC = TestAssemblerPBAC.assemble(DIR+"pbac-assembler-cache-1.ttl");
        assertNotNull(datasetGraphPBAC.labelsStore());
        assertNotNull(datasetGraphPBAC.attributesStore());
        datasetGraphPBAC.close();
    }

    @Test
    public void test_badDurationFormat() {
        Assertions.assertThrows(AssemblerException.class, () ->TestAssemblerPBAC.assemble(DIR+"pbac-assembler-cache-bad-1.ttl"));
    }

    @Test
    public void test_badDurationFormat2() {
        Assertions.assertThrows(AssemblerException.class, () ->TestAssemblerPBAC.assemble(DIR+"pbac-assembler-cache-bad-2.ttl"));
    }


    @Test
    public void test_attributes_initialCallMadeToUnderlyingStoreRemainingToCache() {
        //given
        AttributesStore mockedStore = Mockito.mock(AttributesStore.class);
        when(mockedStore.attributes("user")).thenReturn(SAMPLE_ATTRIBUTE_VALUE_SET)
                                            .thenThrow(new RuntimeException("Test failed - cache bypassed"));
        AttributeStoreCache cut = new AttributeStoreCache(mockedStore, Duration.ofSeconds(10), Duration.ofSeconds(10));
        //when
        AttributeValueSet initialResult = cut.attributes("user");
        AttributeValueSet subsequentResult = cut.attributes("user");
        //then
        assertEquals(initialResult, subsequentResult);
        verify(mockedStore, times(1)).attributes("user");
    }


    @Test
    public void test_getHierarchy_initialCallMadeToUnderlyingStoreRemainingToCache() {
        //given
        AttributesStore mockedStore = Mockito.mock(AttributesStore.class);
        Attribute attribute = new Attribute("Test");
        when(mockedStore.getHierarchy(attribute)).thenReturn(SAMPLE_HIERARCHY)
                                                 .thenThrow(new RuntimeException("Test failed - cache bypassed"));
        AttributeStoreCache cut = new AttributeStoreCache(mockedStore, Duration.ofSeconds(10), Duration.ofSeconds(10));
        //when
        Hierarchy initialResult = cut.getHierarchy(attribute);
        Hierarchy subsequentResult = cut.getHierarchy(attribute);
        //then
        assertEquals(initialResult, subsequentResult);
        verify(mockedStore, times(1)).getHierarchy(attribute);
    }

    @Test
    public void test_hasHierarchy_initialCallMadeToUnderlyingStoreRemainingToCache() {
        // given
        AttributesStore mockedStore = Mockito.mock(AttributesStore.class);
        Attribute attribute = new Attribute("Test");
        when(mockedStore.getHierarchy(attribute)).thenReturn(SAMPLE_HIERARCHY)
                                                 .thenThrow(new RuntimeException("Test failed - cache bypassed"));
        AttributeStoreCache cut = new AttributeStoreCache(mockedStore, Duration.ofSeconds(10), Duration.ofSeconds(10));
        // when
        boolean initialResult = cut.hasHierarchy(attribute);
        boolean subsequentResult = cut.hasHierarchy(attribute);
        //then
        assertEquals(initialResult, subsequentResult);
        verify(mockedStore, times(1)).getHierarchy(attribute);
    }

    @Test
    public void test_hasHierarchy_handleNullResult_asFalse() {
        // given
        AttributesStore mockedStore = Mockito.mock(AttributesStore.class);
        Attribute attribute = new Attribute("Test");
        when(mockedStore.getHierarchy(attribute)).thenReturn(null);
        AttributeStoreCache cut = new AttributeStoreCache(mockedStore, Duration.ofSeconds(10), Duration.ofSeconds(10));
        // when
        boolean result = cut.hasHierarchy(attribute);
        //then
        assertFalse(result);
        verify(mockedStore, times(1)).getHierarchy(attribute);
    }

    @Test
    public void test_hasHierarchy_handleEmptyResult_asFalse() {
        // given
        AttributesStore mockedStore = Mockito.mock(AttributesStore.class);
        Attribute attribute = new Attribute("Test");
        Hierarchy emptyHierarchy = Hierarchy.create(attribute, emptyList());
        when(mockedStore.getHierarchy(attribute)).thenReturn(emptyHierarchy);
        AttributeStoreCache cut = new AttributeStoreCache(mockedStore, Duration.ofSeconds(10), Duration.ofSeconds(10));
        // when
        boolean result = cut.hasHierarchy(attribute);
        //then
        assertFalse(result);
        verify(mockedStore, times(1)).getHierarchy(attribute);
    }

    @Test
    public void test_users_callGoesToUnderlyingStoreNotCache() {
        //given
        AttributesStore mockedStore = Mockito.mock(AttributesStore.class);
        when(mockedStore.users()).thenReturn(Set.of("user1", "user2"))
                                 .thenReturn(Set.of("user1", "user2", "user3", "user4"));
        AttributeStoreCache cut = new AttributeStoreCache(mockedStore, Duration.ofSeconds(10), Duration.ofSeconds(10));
        //when
        Set<String> initialResult = cut.users();
        Set<String> subsequentResult = cut.users();
        //then
        assertNotEquals(initialResult, subsequentResult);
        verify(mockedStore, times(2)).users();
    }

    @Test
    public void test_attributes_initialCallMadeToUnderlyingStoreAfterCacheExpiry() {
        //given
        AttributesStore mockedStore = Mockito.mock(AttributesStore.class);
        when(mockedStore.attributes("user")).thenReturn(SAMPLE_ATTRIBUTE_VALUE_SET)
                                            .thenReturn(AttributeValueSet.of("attribute1=value1"));
        AttributeStoreCache cut = new AttributeStoreCache(mockedStore, Duration.ofNanos(1), Duration.ofNanos(1));
        //when
        AttributeValueSet initialResult = cut.attributes("user");
        AttributeValueSet subsequentResult = cut.attributes("user");
        //then
        assertNotEquals(initialResult, subsequentResult);
        verify(mockedStore, times(2)).attributes("user");
    }
}
