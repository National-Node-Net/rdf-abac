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

package uk.gov.dbt.ndtp.jena.pbac.rocks;

import uk.gov.dbt.ndtp.jena.pbac.PBACTestRunner;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.dbt.ndtp.jena.pbac.labels.Labels;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsException;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStoreRocksDB;
import uk.gov.dbt.ndtp.jena.pbac.labels.StoreFmt;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Run AbstractTestLabelsStore with for the non-rocks labels index setup.
 * This is for consistency checking.
 */
public class TestLabelsStoreMemGraphRocksDB extends AbstractTestLabelsStoreRocksDB {
    @Override
    protected LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        // The graph-based store does not have a mode
        return Labels.createLabelsStoreMem();
    }

    @Override
    protected LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt, Graph graph) {
        // The graph-based store does not have a mode
        LabelsStore s = Labels.createLabelsStoreMem();
        s.addGraph(graph);
        return s;
    }

    private static String labelsGraph = """
        PREFIX foo: <http://example/>
        PREFIX authz: <http://ndtp.co.uk/security#>
        ## No bar:
        [ authz:pattern 'bar:s bar:p1 123' ;  authz:label "allowed" ] .
        """;
    private static Graph BAD_PATTERN = RDFParser.fromString(labelsGraph, Lang.TTL).toGraph();

    @Test
    public void labels_bad_labels_graph() {
        assertThrows(LabelsException.class,
            () -> PBACTestRunner.loggerAtLevel(Labels.LOG, "FATAL",
                () -> createLabelsStore(LabelsStoreRocksDB.LabelMode.MERGE, null, BAD_PATTERN))  // warning and error
        );
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labels_bad_labels_graph(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        assertThrows(LabelsException.class,
            () -> PBACTestRunner.loggerAtLevel(Labels.LOG, "FATAL",
                () -> createLabelsStore(labelMode, storeFmt, BAD_PATTERN))  // warning and error
        );
    }

    /**
     * This is parameterized to catch possible future issues, but currently the parameter is ignored
     * @param labelMode ignored
     */
    @Override
    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labels_add_bad_labels_graph(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        store = createLabelsStore(labelMode, storeFmt);
        String gs = """
            PREFIX : <http://example>
            PREFIX authz: <http://ndtp.co.uk/security#>
            [ authz:pattern 'jibberish' ;  authz:label "allowed" ] .
            """;
        Graph addition = RDFParser.fromString(gs, Lang.TTL).toGraph();

        assertThrows(LabelsException.class,
            () -> PBACTestRunner.loggerAtLevel(Labels.LOG, "FATAL", () -> {
                store.addGraph(addition);
                store.labelsForTriples(triple1);
            }));
    }
}
