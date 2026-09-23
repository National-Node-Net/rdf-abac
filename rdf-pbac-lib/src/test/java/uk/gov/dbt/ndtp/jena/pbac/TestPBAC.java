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

import uk.gov.dbt.ndtp.jena.pbac.lib.AttributesStore;
import uk.gov.dbt.ndtp.jena.pbac.lib.CxtPBAC;
import uk.gov.dbt.ndtp.jena.pbac.lib.DatasetGraphPBAC;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStore;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphZero;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

public class TestPBAC {

    @Test
    public void pbac_is_dataset_pbac_true() {
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        LabelsStore mockLabelsStore = Mockito.mock(LabelsStore.class);
        AttributesStore mockedAttributesStore = Mockito.mock(AttributesStore.class);
        DatasetGraph datasetGraph = new DatasetGraphPBAC(mockDatasetGraph, "attr=1", mockLabelsStore, "test", mockedAttributesStore);
        assertTrue(PBAC.isDatasetPBAC(datasetGraph));
    }

    @Test
    public void pbac_is_dataset_pbac_false() {
        assertFalse(PBAC.isDatasetPBAC(DatasetGraphZero.create()));
    }

    @Test
    public void pbac_request_dataset() {
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        LabelsStore mockLabelsStore = Mockito.mock(LabelsStore.class);
        AttributesStore mockedAttributesStore = Mockito.mock(AttributesStore.class);
        DatasetGraphPBAC datasetGraph = new DatasetGraphPBAC(mockDatasetGraph, "attr=1", mockLabelsStore, "test", mockedAttributesStore);
        DatasetGraph dsg = PBAC.requestDataset(datasetGraph, AttributeValueSet.of("test"), mockedAttributesStore);
        assertNotNull(dsg);
    }

    @Test
    public void pbac_authz_dataset() {
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        LabelsStore mockLabelsStore = Mockito.mock(LabelsStore.class);
        AttributesStore mockedAttributesStore = Mockito.mock(AttributesStore.class);
        DatasetGraphPBAC datasetGraph = new DatasetGraphPBAC(mockDatasetGraph, "attr=1", mockLabelsStore, "test", mockedAttributesStore);
        DatasetGraph dsg = PBAC.authzDataset(datasetGraph, mockLabelsStore, "test", mockedAttributesStore);
        assertNotNull(dsg);
    }

    @Test
    public void pbac_filter_dataset_01() {
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        LabelsStore mockLabelsStore = Mockito.mock(LabelsStore.class);
        CxtPBAC mockContext = Mockito.mock(CxtPBAC.class);
        DatasetGraph dsg = PBAC.filterDataset(mockDatasetGraph, mockLabelsStore, "test", mockContext);
        assertNotNull(dsg);
    }

    @Test
    public void pbac_filter_dataset_02() {
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtPBAC mockContext = Mockito.mock(CxtPBAC.class);
        DatasetGraph dsg = PBAC.filterDataset(mockDatasetGraph, null, "test", mockContext);
        assertNotNull(dsg);
    }

    @Test
    public void pbac_read_shacl() {
        Shapes shapes = PBAC.readSHACL("TestShape.ttl");
        assertNotNull(shapes);
    }
}
