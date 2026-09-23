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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.gov.dbt.ndtp.jena.pbac.lib.DatasetGraphPBAC;
import org.apache.jena.atlas.lib.FileOps;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.fuseki.Fuseki;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.FusekiTestLib;
import org.apache.jena.fuseki.main.sys.FusekiModule;
import org.apache.jena.fuseki.main.sys.FusekiModules;
import org.apache.jena.fuseki.system.FusekiLogging;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.exec.QueryExec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.dbt.ndtp.jena.pbac.fuseki.FMod_PBAC;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test Fuseki + FMod_PBAC
 */
public class TestServer_FMod_PBAC {
    static {
        FusekiLogging.setLogging();
    }

    private static final String DIR = "src/test/files/server/";

    private static final String PREFIXES = StrUtils.strjoinNL
            ("PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
            ,"PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>"
            ,"PREFIX sh:      <http://www.w3.org/ns/shacl#>"
            ,"PREFIX xsd:     <http://www.w3.org/2001/XMLSchema#>"
            ,"PREFIX : <http://example/>"
            ,""
             );

    static FusekiModule modulePBAC;

    private static FusekiModules modules;

    private static FusekiServer server(String configFile) {
        return FusekiServer.create().port(0)
                .fusekiModules(modules)
                .parseConfigFile(FileOps.concatPaths(DIR,configFile))
                .build();
    }

    // ----
    // This ought to go somewhere! Undo in TestRDFLinkRemote.
    // Solves the problem that src/test/resources/log4j2.properties can be loaded
    // from the main classpath before the test resources.
    private static final String[] loggers = {
        Fuseki.serverLogName,
        Fuseki.actionLogName,
        Fuseki.requestLogName,
        Fuseki.adminLogName,
        "org.eclipse.jetty"
    };

    private static void silentAll(Runnable action) {
        silentAll(action, loggers, "ERROR");
    }

    private static void silentAll(Runnable action, String[] loggers, String runLevel) {
        Map<String, String> levels = new HashMap<>();
        for ( String logger : loggers ) {
            levels.put(logger, LogCtl.getLevel(logger));
            //LogCtl.disable(logger);
            LogCtl.setLevel(logger, runLevel);
        }
        try {
            action.run();
        } finally {
            levels.forEach(LogCtl::setLevel);
        }
    }
    // ----

    @BeforeAll
    public static void setup() {
//        Function<HttpAction, String> getUserFunction = a->"u1";
//        modulePBAC = FMod_PBAC.testSetup(getUserFunction);
        modulePBAC = new FMod_PBAC();
        modules = FusekiModules.create(List.of(modulePBAC));
    }

    @AfterAll
    public static void cleanup() {
        // Placeholder method
    }

    @Test public void build_labels() {
        FusekiServer server = server("server-labels/config-labels.ttl");
        DatasetGraph dsg0 = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphPBAC dsgz = (DatasetGraphPBAC)dsg0;
        assertNotNull(dsgz.labelsStore());
        assertFalse(dsgz.labelsStore().isEmpty());

        assertNull(dsgz.getAccessAttributes());
        assertNotNull(dsgz.getDefaultLabel());
        assertEquals("default", dsgz.getDefaultLabel());
    }

    @Test public void build_run_server() {
        FusekiServer server = server("server-labels/config-labels.ttl");
        DatasetGraph dsg0 = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphPBAC dsgz = (DatasetGraphPBAC)dsg0;
        server.start();
        try {
            String URL="http://localhost:"+server.getPort()+"/ds";
            String URL_BASE="http://localhost:"+server.getPort()+"/base";

            // No user at all -> 400.
            silentAll(()-> {
                FusekiTestLib.expectQuery400( ()-> QueryExec.service(URL).query("ASK{}").ask());
            });
        } finally { server.stop(); }
    }

    // Check that a PBAC dataset causes override of the action processors for operations in the fuseki service.
    // The config uses fuseki:query/fuseki:upload, not authz:query/authz:upload,
    @Test public void build_run_server_override() {
        FusekiServer server = server("config-server-plain.ttl");
        DatasetGraph dsg0 = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphPBAC dsgz = (DatasetGraphPBAC)dsg0;
        server.start();
        try {
            String URL="http://localhost:"+server.getPort()+"/ds";
            String URL_BASE="http://localhost:"+server.getPort()+"/base";

            // No user at all -> 400.
            silentAll(()-> {
                FusekiTestLib.expectQuery400( ()-> QueryExec.service(URL).query("ASK{}").ask());
            });

            // But can access the other dataset without any security.
            QueryExec.service(URL_BASE).query("ASK{}").ask();
        } finally { server.stop(); }
    }
}
