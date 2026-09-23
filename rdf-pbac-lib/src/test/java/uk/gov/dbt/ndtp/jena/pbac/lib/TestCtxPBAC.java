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
import uk.gov.dbt.ndtp.jena.pbac.attributes.ValueTerm;
import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.sparql.core.DatasetGraph;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TestCtxPBAC {

    @Test
    public void test_create_eval_cache_zero() {
        Cache<String, ValueTerm> cache = CxtPBAC.createEvalCache(0);
        assertEquals(0, cache.size());
    }

    @Test
    public void test_create_hierarchy_cache_zero() {
        Cache<Attribute, Optional<Hierarchy>> cache = CxtPBAC.createHierarchyCache(0);
        assertEquals(0, cache.size());
    }

    @Test
    public void test_data() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtPBAC cxtPBAC = CxtPBAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        assertNotNull(cxtPBAC.data());
    }

    @Test
    public void test_request_id() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtPBAC cxtPBAC = CxtPBAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        assertNotNull(cxtPBAC.requestId());
    }

    @Test
    public void test_tracking_none() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtPBAC cxtPBAC = CxtPBAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtPBAC.tracking(Track.NONE);
        assertEquals(Track.NONE, cxtPBAC.tracking());
    }

    @Test
    public void test_tracking_debug() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtPBAC cxtPBAC = CxtPBAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtPBAC.tracking(Track.DEBUG);
        assertEquals(Track.DEBUG, cxtPBAC.tracking());
    }

    @Test
    public void test_tracking_trace() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtPBAC cxtPBAC = CxtPBAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtPBAC.tracking(Track.TRACE);
        assertEquals(Track.TRACE, cxtPBAC.tracking());
    }

    @Test
    public void test_debug_true() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtPBAC cxtPBAC = CxtPBAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtPBAC.tracking(Track.DEBUG);
        assertTrue(cxtPBAC.debug());
    }

    @Test
    public void test_debug_true_with_trace() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtPBAC cxtPBAC = CxtPBAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtPBAC.tracking(Track.TRACE);
        assertTrue(cxtPBAC.debug());
    }

    @Test
    public void test_debug_false() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtPBAC cxtPBAC = CxtPBAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtPBAC.tracking(Track.NONE);
        assertFalse(cxtPBAC.debug());
    }

    @Test
    public void test_trace_true() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtPBAC cxtPBAC = CxtPBAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtPBAC.tracking(Track.TRACE);
        assertTrue(cxtPBAC.trace());
    }

    @Test
    public void test_trace_false() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtPBAC cxtPBAC = CxtPBAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtPBAC.tracking(Track.NONE);
        assertFalse(cxtPBAC.trace());
    }

    @Test
    public void test_system_trace() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtPBAC.systemTrace(Track.NONE);
        CxtPBAC cxtPBAC = CxtPBAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        assertEquals(Track.NONE, cxtPBAC.tracking());
    }
}
