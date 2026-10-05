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

package uk.gov.dbt.ndtp.jena.abac.lib;

import uk.gov.dbt.ndtp.jena.abac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.abac.Hierarchy;
import uk.gov.dbt.ndtp.jena.abac.attributes.Attribute;
import uk.gov.dbt.ndtp.jena.abac.attributes.ValueTerm;
import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.sparql.core.DatasetGraph;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TestCtxABAC {

    @Test
    public void test_create_eval_cache_zero() {
        Cache<String, ValueTerm> cache = CxtABAC.createEvalCache(0);
        assertEquals(0, cache.size());
    }

    @Test
    public void test_create_hierarchy_cache_zero() {
        Cache<Attribute, Optional<Hierarchy>> cache = CxtABAC.createHierarchyCache(0);
        assertEquals(0, cache.size());
    }

    @Test
    public void test_data() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        assertNotNull(cxtABAC.data());
    }

    @Test
    public void test_request_id() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        assertNotNull(cxtABAC.requestId());
    }

    @Test
    public void test_tracking_none() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtABAC.tracking(Track.NONE);
        assertEquals(Track.NONE, cxtABAC.tracking());
    }

    @Test
    public void test_tracking_debug() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtABAC.tracking(Track.DEBUG);
        assertEquals(Track.DEBUG, cxtABAC.tracking());
    }

    @Test
    public void test_tracking_trace() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtABAC.tracking(Track.TRACE);
        assertEquals(Track.TRACE, cxtABAC.tracking());
    }

    @Test
    public void test_debug_true() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtABAC.tracking(Track.DEBUG);
        assertTrue(cxtABAC.debug());
    }

    @Test
    public void test_debug_true_with_trace() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtABAC.tracking(Track.TRACE);
        assertTrue(cxtABAC.debug());
    }

    @Test
    public void test_debug_false() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtABAC.tracking(Track.NONE);
        assertFalse(cxtABAC.debug());
    }

    @Test
    public void test_trace_true() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtABAC.tracking(Track.TRACE);
        assertTrue(cxtABAC.trace());
    }

    @Test
    public void test_trace_false() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtABAC.tracking(Track.NONE);
        assertFalse(cxtABAC.trace());
    }

    @Test
    public void test_system_trace() {
        AttributeValueSet mockAttributeValueSet = Mockito.mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = Mockito.mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtABAC.systemTrace(Track.NONE);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        assertEquals(Track.NONE, cxtABAC.tracking());
    }
}
