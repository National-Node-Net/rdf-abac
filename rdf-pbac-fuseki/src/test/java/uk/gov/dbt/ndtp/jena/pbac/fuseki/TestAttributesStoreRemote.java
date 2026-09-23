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


package uk.gov.dbt.ndtp.jena.pbac.fuseki;

import static uk.gov.dbt.ndtp.jena.pbac.services.LibAuthService.serviceURL;
import static org.junit.jupiter.api.Assertions.assertEquals;

import uk.gov.dbt.ndtp.jena.pbac.PBAC;
import uk.gov.dbt.ndtp.jena.pbac.lib.Attributes;
import uk.gov.dbt.ndtp.jena.pbac.lib.AttributesStore;
import uk.gov.dbt.ndtp.jena.pbac.labels.Labels;
import uk.gov.dbt.ndtp.jena.pbac.services.AttributeService;
import uk.gov.dbt.ndtp.jena.pbac.services.SimpleAttributesStore;
import org.apache.jena.atlas.lib.FileOps;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.cmds.FusekiMain;
import org.apache.jena.fuseki.main.sys.FusekiModules;
import org.apache.jena.fuseki.system.FusekiLogging;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.sparql.exec.RowSet;
import org.apache.jena.sparql.exec.RowSetOps;
import org.apache.jena.sparql.exec.http.DSP;
import org.apache.jena.sparql.exec.http.QueryExecHTTPBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Tests for a remote attribute store. */
public class TestAttributesStoreRemote {

    private static final String DIR = "src/test/files/integration";

    protected static String level = null;

    private static FusekiServer server(String configFile) {
        return FusekiServer.create().port(0)
                .fusekiModules(FusekiModules.create(new FMod_PBAC()))
                .parseConfigFile(FileOps.concatPaths(DIR,"server-labels/config-labels.ttl"))
                .build();
    }

    @BeforeAll
    public static void beforeAll() {
        // See also src/test/resources/log4j2.properties
        FusekiLogging.setLogging();
        level = LogCtl.getLevel(PBAC.AttrLOG);
        LogCtl.disable(PBAC.AttrLOG);
    }

    @AfterAll
    public static void afterAll() {
        LogCtl.setLevel(PBAC.AttrLOG, level);
    }

    @Test public void integration_1() {
        withFilterLogging(false,
                            ()->runTest("config-java-services.ttl", 2));
    }

    // Missing matches are hard to find without filter logging, but that can be verbose to have on.
    // See also src/test/resources/log4j2.properties
    private void withFilterLogging(boolean value, Runnable action) {
        Labels.setLabelFilterLogging(value);
        try {
            action.run();
        } finally { Labels.setLabelFilterLogging(false); }
    }

    @Test public void integration_2() {
        withFilterLogging(false,
                            ()->runTest("config-no-hierarchies.ttl", 1));
    }

    private static void runTest(String configFile, int expected) {
        // Remote Attribute Store
        Graph g = RDFParser.source(DIR+"/attribute-store.ttl").toGraph();
        AttributesStore attrStore = Attributes.buildStore(g);
        String mockServerURL = SimpleAttributesStore.run(0, attrStore);

        String attributeStoreBaseURL = mockServerURL;
        String lookupUserAttributesURL = serviceURL(attributeStoreBaseURL, AttributeService.LOOKUP_USER_ATTRIBUTE_TEMPLATE);
        String lookupHierarchAttributesURL = serviceURL(attributeStoreBaseURL, AttributeService.LOOKUP_HIERARCHY_TEMPLATE);

        System.setProperty("USER_ATTRIBUTES_URL", lookupUserAttributesURL);
        System.setProperty("PBAC_HIERARCHIES_URL", lookupHierarchAttributesURL);

        FusekiServer server = FusekiMain
                .builder("--port=0", "--conf", DIR+"/"+configFile)
                .fusekiModules(FusekiModules.create(new FMod_PBAC()))
                .start();
        try {
            int port = server.getHttpPort();
            String URL = "http://localhost:" + port + "/ds";
            DSP.service(URL+"/upload").POST(DIR+"/data-hierarchies.trig");

            RowSet rs = query(URL, "SELECT * {?s ?p ?o}", "user1@email");
            //RowSetOps.out(rs);
            // With hierarchies: 2
            // Without hierarchies: 1
            long x = RowSetOps.count(rs);
            assertEquals(expected, x);
        } finally {
            if ( server != null )
                server.stop();
        }
    }

    public static RowSet query(String url, String queryString, String user) {
        String qs = queryString;
        RowSet rs1 =
                QueryExecHTTPBuilder.service(url)
                .httpHeader("Authorization", "Bearer user:"+user)
                .query(qs)
                .select();
        return rs1;
    }
}
