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

import uk.gov.dbt.ndtp.jena.abac.lib.AttributesStore;
import uk.gov.dbt.ndtp.jena.abac.lib.CxtABAC;
import uk.gov.dbt.ndtp.jena.abac.lib.DatasetGraphABAC;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStore;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphZero;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

public class TestABAC {

    @Test
    public void abac_is_dataset_abac_true() {
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        LabelsStore mockLabelsStore = Mockito.mock(LabelsStore.class);
        AttributesStore mockedAttributesStore = Mockito.mock(AttributesStore.class);
        DatasetGraph datasetGraph = new DatasetGraphABAC(mockDatasetGraph, "attr=1", mockLabelsStore, "test", mockedAttributesStore);
        assertTrue(ABAC.isDatasetABAC(datasetGraph));
    }

    @Test
    public void abac_is_dataset_abac_false() {
        assertFalse(ABAC.isDatasetABAC(DatasetGraphZero.create()));
    }

    @Test
    public void abac_request_dataset() {
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        LabelsStore mockLabelsStore = Mockito.mock(LabelsStore.class);
        AttributesStore mockedAttributesStore = Mockito.mock(AttributesStore.class);
        DatasetGraphABAC datasetGraph = new DatasetGraphABAC(mockDatasetGraph, "attr=1", mockLabelsStore, "test", mockedAttributesStore);
        DatasetGraph dsg = ABAC.requestDataset(datasetGraph, AttributeValueSet.of("test"), mockedAttributesStore);
        assertNotNull(dsg);
    }

    @Test
    public void abac_authz_dataset() {
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        LabelsStore mockLabelsStore = Mockito.mock(LabelsStore.class);
        AttributesStore mockedAttributesStore = Mockito.mock(AttributesStore.class);
        DatasetGraphABAC datasetGraph = new DatasetGraphABAC(mockDatasetGraph, "attr=1", mockLabelsStore, "test", mockedAttributesStore);
        DatasetGraph dsg = ABAC.authzDataset(datasetGraph, mockLabelsStore, "test", mockedAttributesStore);
        assertNotNull(dsg);
    }

    @Test
    public void abac_filter_dataset_01() {
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        LabelsStore mockLabelsStore = Mockito.mock(LabelsStore.class);
        CxtABAC mockContext = Mockito.mock(CxtABAC.class);
        DatasetGraph dsg = ABAC.filterDataset(mockDatasetGraph, mockLabelsStore, "test", mockContext);
        assertNotNull(dsg);
    }

    @Test
    public void abac_filter_dataset_02() {
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtABAC mockContext = Mockito.mock(CxtABAC.class);
        DatasetGraph dsg = ABAC.filterDataset(mockDatasetGraph, null, "test", mockContext);
        assertNotNull(dsg);
    }

    @Test
    public void abac_read_shacl() {
        Shapes shapes = ABAC.readSHACL("TestShape.ttl");
        assertNotNull(shapes);
    }
}
