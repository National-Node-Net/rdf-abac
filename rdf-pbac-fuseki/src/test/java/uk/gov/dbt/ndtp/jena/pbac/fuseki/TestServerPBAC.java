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

import java.io.File;
import java.net.Authenticator;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import uk.gov.dbt.ndtp.jena.pbac.PBACTestRunner;
import uk.gov.dbt.ndtp.jena.pbac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.pbac.lib.DatasetGraphPBAC;
import uk.gov.dbt.ndtp.jena.pbac.lib.LabelledDataWriter;
import uk.gov.dbt.ndtp.jena.pbac.labels.Labels;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStoreRocksDB;
import org.apache.jena.atlas.lib.FileOps;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.fuseki.Fuseki;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.FusekiTestLib;
import org.apache.jena.fuseki.main.sys.FusekiModule;
import org.apache.jena.fuseki.main.sys.FusekiModules;
import org.apache.jena.fuseki.system.FusekiLogging;
import org.apache.jena.graph.Graph;
import org.apache.jena.http.HttpRDF;
import org.apache.jena.http.auth.AuthLib;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.exec.RowSet;
import org.apache.jena.sparql.exec.RowSetOps;
import org.apache.jena.sparql.exec.RowSetRewindable;
import org.apache.jena.sparql.exec.http.DSP;
import org.apache.jena.sparql.exec.http.GSP;
import org.apache.jena.sparql.exec.http.QueryExecHTTPBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test Fuseki+PBAC with general data from an
 */
public class TestServerPBAC {
    static {
        FusekiLogging.setLogging();
    }

    // Some tests have authz:tripleDefaultLabels "*";
    // some do not.
    // u1 can see 3 triples with this and 2 without it
    // u3 can see 1 triple1 with this and 0 without it

    private static final String DIR = "src/test/files/server/";

    private static final String PREFIXES =
            """
            PREFIX : <http://example/>
            PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX sh:    <http://www.w3.org/ns/shacl#>
            PREFIX xsd:   <http://www.w3.org/2001/XMLSchema#>
            PREFIX :      <http://example/>

             """;

    private enum WithDebug {DEBUG, PLAIN}

    private final static WithDebug plain = WithDebug.PLAIN;
    private final static WithDebug debug = WithDebug.DEBUG;

    // ----

    private static final String[] loggersWarn = {
        Fuseki.serverLogName,
        Fuseki.adminLogName,
        Fuseki.requestLogName,
        "org.eclipse.jetty"
    };

    private static final String[] loggersAction = {
        Fuseki.actionLogName
    };

    private static void silentAll(Runnable action) {
        silent("WARN", loggersWarn, action);
        silent("OFF",  loggersAction, action);
    }

    private static void silent(String runLevel, String[] loggers, Runnable action) {
        Map<String, String> levels = new HashMap<>();
        for ( String logger : loggers ) {
            levels.put(logger, LogCtl.getLevel(logger));
            LogCtl.setLevel(logger, runLevel);
        }
        try {
            action.run();
        } finally {
            levels.forEach(LogCtl::setLevel);
        }
    }

    // ----

//    @BeforeAll
//    public static void setup() {}
//
//    @AfterAll
//    public static void cleanup() {}

    private static FusekiServer server(String config) {
        FusekiModule fmod = new FMod_PBAC();
        FusekiModules mods = FusekiModules.create(fmod);
        return FusekiServer.create()
                .port(0)
                .fusekiModules(mods)
                .parseConfigFile(FileOps.concatPaths(DIR, config))
                .build();
    }

    private static FusekiServer server(Graph configuration) {
        FusekiModule fmod = new FMod_PBAC();
        FusekiModules mods = FusekiModules.create(fmod);
        return FusekiServer.create()
                .port(0)
                .fusekiModules(mods)
                .parseConfig(configuration)
                .build();
    }

    // This file location is known to some assembler files.
    private static final String DIR_NAME_LABELS_STORE = "target/LabelsStore.db";

    private static void noLabelStoreDirectory(String dirName) {
        FileOps.clearDirectory(dirName);
        FileOps.delete(dirName);

        File f = new File(dirName);
        @SuppressWarnings("resource")
        LabelsStoreRocksDB db = Labels.rocks.get(f);
        if ( db != null) {
            try {
                db.close();
            } catch (Exception ex) { throw new RuntimeException(ex);}
        }
    }

    @Test public void build() {
        FusekiServer server = server("config-server.ttl");
        DatasetGraph dsg0 = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphPBAC dsgz = (DatasetGraphPBAC)dsg0;
        assertNotNull(dsgz.labelsStore());
        assertTrue(dsgz.labelsStore().isEmpty());
        server.start();
        server.stop();
    }

    // No load only tests - can't determine the success or failure without a query.

    @Test public void query_u1() {
        FusekiServer server = server("config-server.ttl");
        server.start();
        String URL = "http://localhost:"+server.getPort()+"/ds";
        try {
            load(server);
            query(plain, URL, "u1", 3);
        } finally { server.stop(); }
    }

    @Test public void query_u1_u2() {
        FusekiServer server = server("config-server.ttl");
        server.start();
        String URL = "http://localhost:"+server.getPort()+"/ds";
        try {
            load(server);
            query(plain, URL, "u1", 3);
            query(plain, URL, "u2", 2);
        } finally { server.stop(); }
    }

    @Test public void query_anon() {
        FusekiServer server = server("config-server.ttl");
        server.start();
        String URL = "http://localhost:"+server.getPort()+"/ds";
        try {
            load(server);
            silent("OFF", loggersAction,
                   ()->FusekiTestLib.expectQuery403( ()-> query(plain, URL, "anon", 0) ) );
        } finally { server.stop(); }
    }

    @Test public void query_dft_access() {
        FusekiServer server = server("config-server-dft-label.ttl");
        server.start();
        String URL = "http://localhost:"+server.getPort()+"/ds";
        try {
            load(server);
            query(plain, URL, "u1", PREFIXES+"SELECT * { ?s :q ?o }", 0);
            query(plain, URL, "u2", PREFIXES+"SELECT * { ?s :q ?o }", 1);
        } finally { server.stop(); }
    }

    @Test public void query_access() {
        FusekiServer server = server("config-server-access.ttl");
        server.start();
        String URL = "http://localhost:"+server.getPort()+"/ds";
        try {
            // No data, no labels.
            // u1 can not access
            silent("ERROR", loggersAction,
                   ()->FusekiTestLib.expectQuery403( ()-> query(plain, URL, "u1", 0) ) );
            // u2 can access
            query(plain, URL, "u2", 0);
        } finally { server.stop(); }
    }

    @Test public void gsp_r_1() {
        FusekiServer server = server("config-server-gspr.ttl");
        server.start();
        String baseURL = "http://localhost:"+server.getPort();
        DSP.service(baseURL+"/ds/upload").POST(DIR+"data-and-labels.trig");

        try {
            // Try all the endpoints.
            Graph g11 = gspGET(plain, baseURL+"/ds", "u1", 3);
            Graph g12 = gspGET(plain, baseURL+"/ds/gsp-r-plain", "u1", 3);
            Graph g13 = gspGET(plain, baseURL+"/ds/gsp-r-authz", "u1", 3);
            assertTrue(g11.isIsomorphicWith(g12));
            assertTrue(g11.isIsomorphicWith(g13));

            Graph g2 = gspGET(plain, baseURL+"/ds", "u2", 2);
            assertFalse(g11.isIsomorphicWith(g2));
            // u3 can access but can only see the unlabelled triple.
            gspGET(plain, baseURL+"/ds/gsp-r-plain", "u3", 1);
        } finally { server.stop(); }
    }

    @Test public void gsp_r_2() {
        FusekiServer server = server("config-server-gspr.ttl");
        server.start();
        String baseURL = "http://localhost:"+server.getPort();
        DSP.service(baseURL+"/ds/upload").POST(DIR+"data-and-labels.trig");

        try {
            Graph g1 = gspGET(plain, baseURL+"/ds", "u1", 3);
            Graph g2 = gspGET(plain, baseURL+"/ds", "u2", 2);
            assertFalse(g1.isIsomorphicWith(g2));
            // u3 can access but can only see the unlabelled triple.
            gspGET(plain, baseURL+"/ds/gsp-r-plain", "u3", 1);
        } finally { server.stop(); }
    }

    // -- Check security applied for plain (fuseki:) forms and authz: forms.

    // Configuration: fuseki:query/fuseki:upload forms.
    @Test public void config_override_plain() {
        config_override("config-server-plain.ttl");
    }

    // Configuration: preferred form (fuseki:query, authz:upload)
    @Test public void config_override_preferred() {
        config_override("config-server.ttl");
    }

    private void config_override(String configFile) {
        FusekiServer server = server(configFile);
        server.start();
        String URL = "http://localhost:"+server.getPort()+"/ds";
        DSP.service(URL+"/upload").POST(DIR+"data-and-labels.trig");
        // u2 can access
        query(plain, URL, "u2", 2);
        gspGET(plain, URL, "u2", 2);
        // u3 can access and can see only the unlabelled label.
        query(plain, URL, "u3", 1);
        gspGET(plain, URL, "u3", 1);

        // ---- The plain dataset
        String URL2 = "http://localhost:"+server.getPort()+"/base";
        boolean b = QueryExecHTTPBuilder.service(URL2).query("ASK {}").ask();
    }

    // Configuration: authz:query/authz:upload forms.
    // Custom operations don't allow for multi-operation dispatch on an endpoint
    // (only supported for the SPARQL standard forms).
    // Must use endpoint with a single operations.
    @Test public void config_override_custom_forms() {
        FusekiServer server = server("config-server-authz.ttl");
        server.start();
        try {
            String baseURL = "http://localhost:"+server.getPort()+"/ds";
            DSP.service(baseURL+"/upload").POST(DIR+"data-and-labels.trig");

            // u2 can access
            query(plain, baseURL+"/query", "u2", 1);
            gspGET(plain, baseURL+"/gsp-r", "u2", 1);
            // u3 can access and can see only the unlabelled label but that is "deny"
            query(plain, baseURL+"/query", "u3", 0);
            gspGET(plain, baseURL+"/gsp-r", "u3", 0);

            // ---- The plain dataset
            String URL2 = "http://localhost:"+server.getPort()+"/base";
            boolean b = QueryExecHTTPBuilder.service(URL2).query("ASK {}").ask();
        } finally { server.stop(); }
    }

    @Test public void persistent_server_1() {
        // This is know to the server configuration file.
        assertDoesNotThrow(() -> {
            noLabelStoreDirectory(DIR_NAME_LABELS_STORE);
            FusekiServer server = server("config-persistent-server-1.ttl");
        });
    }

    @Test public void server_restart_1() {
        assertDoesNotThrow(() -> {
            server_restart("config-persistent-server-1.ttl");
        });
    }

    private void server_restart(String configurationFile) {
        // Clear databases

        FileOps.ensureDir(DATABASE_AREA);
        FileOps.clearAll(DATABASE_AREA);

        {
            // Empty server
            FusekiServer server1 = server(configurationFile);
            server1.start();
            try {
                String URL="http://localhost:"+server1.getPort()+"/ds";
                query(plain, URL, "u1", 0);
                query(plain, URL, "u3", 0);
                load(server1);
                query(plain, URL, "u1", 2);
                query(plain, URL, "u3", 0);
            } finally { server1.stop(); }
        }

        {
            // restart
            FusekiServer server2 = server(configurationFile);
            server2.start();
            try {
                String URL="http://localhost:"+server2.getPort()+"/ds";
                query(plain, URL, "u1", 2);
                query(plain, URL, "u3", 0);
            } finally { server2.stop(); }
        }
    }

    // Authentication using Jetty for login.
    @Test public void config_server_auth() {
        FusekiServer server = server("config-jetty-auth.ttl");
                // In config file.
                //   .passwordFile(DIR+"jetty-passwd")
                //   .auth(AuthScheme.BASIC)
        server.start();
        try {
            String URL = "http://localhost:"+server.getPort()+"/ds";
            // Only u1 is in the password file.

            HttpClient httpClient_u1 = createHttpClient("u1", "pw1");
            HttpClient httpClient_u2 = createHttpClient("u2", "pw2");

            DSP.service(URL+"/upload").httpClient(httpClient_u1).POST(DIR+"data-and-labels.trig");
            // u2 can not access the server
            FusekiTestLib.expectQuery401(()->queryWithHttpClient(plain, URL, httpClient_u2, "u2", 3));
            // u1 can access
            queryWithHttpClient(plain, URL, httpClient_u1, "u1", 2);
        } finally { server.stop(); }
    }

    private HttpClient createHttpClient(String user, String password) {
        Authenticator authenticator1 = AuthLib.authenticator(user, password);
        return HttpClient.newBuilder()
                .authenticator(authenticator1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Test public void get_labels() {
        FusekiServer server = server("config-server-authz.ttl");
        server.start();
        try {
            String URL="http://localhost:"+server.getPort()+"/ds";
            Graph g1 = HttpRDF.httpGetGraph(URL+"/labels", null);
            assertTrue(g1.isEmpty(),"Expected no labels triples in empty dataset");

            load(server);

            Graph g2 = HttpRDF.httpGetGraph(URL+"/labels", null);
            assertFalse(g2.isEmpty(), "Expected labels triples");
        } finally { server.stop(); }
    }

    private static final String DATABASE_AREA = "target/databases";

    @Test public void server_all() {
        FusekiServer server = server("all/config-all.ttl");
        server.start();
        try {

            String URL="http://localhost:"+server.getPort()+"/ds";
            Graph g1 = HttpRDF.httpGetGraph(URL+"/labels", null);
            assertTrue(g1.isEmpty(), "Expected no labels triples in empty dataset");
            // Check nothing.
            query(plain, URL, "u1", 0);

            PlayLib.sendStringHTTP(URL+"/upload", ConstForTestServerPBAC.dataWithLabelsTriG);
            Graph g2 = HttpRDF.httpGetGraph(URL+"/labels", null);
            assertFalse(g2.isEmpty(), "Expected labels triples in label graph");

            query(plain, URL, "u1", 7);
            query(plain, URL, "u2", 4);
            query(plain, URL, "u3", 1);
        } finally { server.stop(); }
    }

    @Test public void labelled_data_trig_01() {
        // With authz:tripleDefaultLabels     "*" ;
        test_dataByTrig(ConstForTestServerPBAC.configServer+ ConstForTestServerPBAC.configDatasetDftLabelStar, 7, 4, 1);
    }

    @Test public void labelled_data_trig_02() {
        // Without authz:tripleDefaultLabels
        test_dataByTrig(ConstForTestServerPBAC.configServer+ ConstForTestServerPBAC.configDatasetNoDftLabel, 6, 3, 0);
    }

    private void test_dataByTrig(String config, int countU1, int countU2, int countU3) {
        FusekiServer server = serverConfigString(config).start();;
        try {
            String URL ="http://localhost:"+server.getPort()+"/ds";
            loadData(server, List.of(ConstForTestServerPBAC.dataWithLabelsTriG));
            query(server, URL, countU1, countU2, countU3);
        } finally { server.stop(); }
    }

    /*
     *   u1 :: AttributeValueSet[manager=true, status=sensitive]
     *   u2 :: AttributeValueSet[engineer=true, status=public]
     *   u3 :: AttributeValueSet[nothing=true]
     *   status: [public, confidential, sensitive, private]
     */

    // Tests where data is by triples and security header.
    @Test public void labelled_data_triples_01() {
        // With authz:tripleDefaultLabels     "*" ;
        test_dataByTriples(ConstForTestServerPBAC.configServer+ ConstForTestServerPBAC.configDatasetDftLabelStar, 5, 3, 1);
    }

    @Test public void labelled_data_triples_02() {
        // No authz:tripleDefaultLabels -> deny?
        test_dataByTriples(ConstForTestServerPBAC.configServer+ ConstForTestServerPBAC.configDatasetNoDftLabel, 5, 3, 1);
    }

    @Test public void labelled_data_triples_03() {
        // No authz:tripleDefaultLabels status=confidential
        test_dataByTriples(
            ConstForTestServerPBAC.configServer+ ConstForTestServerPBAC.configDatasetDfLabelConfidential, 5, 3, 1);
    }

    /** Data files for testing triples loading. */
    private static List<String> dataTriples =
            List.of(
                    //dataTriplesLabelNone,
                    ConstForTestServerPBAC.dataTriplesLabelDeny,
                    ConstForTestServerPBAC.dataTriplesLabelEmpty,
                    ConstForTestServerPBAC.dataTriplesLabelOpen,

                    ConstForTestServerPBAC.payloadTriplesLabelPublic,
                    ConstForTestServerPBAC.payloadTriplesLabelConfidential,
                    ConstForTestServerPBAC.payloadTriplesLabelSensitive,
                    ConstForTestServerPBAC.payloadTriplesLabelPrivate
                    );

    private void test_dataByTriples(String config, int countU1, int countU2, int countU3) {
        FusekiServer server = serverConfigString(config).start();
        try {
            String URL ="http://localhost:"+server.getPort()+"/ds";
            loadData(server, dataTriples);
            query(server, URL, countU1, countU2, countU3);
        } finally { server.stop(); }
    }

    private FusekiServer serverConfigString(String config) {
            Graph configGraph = RDFParser.fromString(config, Lang.TTL).toGraph();
            FusekiServer server = server(configGraph);
            return server;
        }

    private static void loadData(FusekiServer server, List<String> data) {
        String URL ="http://localhost:"+server.getPort()+"/ds/upload";
        data.forEach(fn->PlayLib.sendStringHTTP(URL, fn));
    }

    private void query(FusekiServer server, String queryServiceURL, int countU1, int countU2, int countU3) {
        WithDebug dbgSetup = plain;
        WithDebug dbgQuery = plain;
        if ( dbgSetup == debug  ) {
            DatasetGraphPBAC dsgz = (DatasetGraphPBAC)server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
            Consumer<String> printUser = u -> {
                AttributeValueSet avs = dsgz.attributesStore().attributes(u);
                System.out.printf("User %s :: %s\n", u, avs);
            };
            System.out.println("--");
            LabelledDataWriter.writeWithLabels(System.out, dsgz);
            printUser.accept("u1");
            printUser.accept("u2");
            printUser.accept("u3");
            System.out.println("--");
            System.out.printf("Expected counts: u1=%d, u2=%d, u3=%d\n", countU1, countU2, countU3);
        }
        query(dbgQuery, queryServiceURL, "u1", countU1);
        query(dbgQuery, queryServiceURL, "u2", countU2);
        query(dbgQuery, queryServiceURL, "u3", countU3);
    }

    private void load(FusekiServer server) {
        String URL = "http://localhost:"+server.getPort()+"/ds";
        String uploadURL = URL+"/upload";
        load(uploadURL, DIR+"data-and-labels.trig");
    }

    private void load(String uploadURL, String filename) {
        DSP.service(uploadURL).POST(filename);
    }

    private void query(WithDebug debug, String url, String user, int expectedCount) {
        String queryString = "SELECT * { ?s ?p ?o }";
        query(debug, url, user, queryString, expectedCount);
    }

    private void queryWithHttpClient(WithDebug debug, String url,  HttpClient httpClient, String user, int expectedCount) {
        String queryString = "SELECT * { ?s ?p ?o }";
        queryWithHttpClient(debug, url, httpClient, user, queryString, expectedCount);
    }

    private void query(WithDebug debug, String url, String user, String queryString, int expectedCount) {
        boolean withDebug = (debug == WithDebug.DEBUG);
        if ( withDebug )
            System.out.println("Query: "+user);
        PBACTestRunner.debugPBAC(withDebug, ()->{
            // == PBAC Query
            RowSetRewindable rowSet =
                    QueryExecHTTPBuilder.service(url)
                    .query(queryString)
                    .httpHeader("Authorization", "Bearer user:"+user)
                    .select()
                    .rewindable();
            long x = RowSetOps.count(rowSet);
            if ( expectedCount != x ) {
                System.out.printf("Expected = %d ; actual = %d\n", expectedCount, x);
                System.out.println("User = "+user);
                rowSet.reset();
                RowSetOps.out(System.out, rowSet);
            }
            assertEquals(expectedCount, x, "Count mismatch (user='"+user+"')");
        });
    }

    private Graph gspGET(WithDebug debug, String URL, String user, int expectedCount) {
        boolean withDebug = (debug == WithDebug.DEBUG);
        return PBACTestRunner.calcDebugPBAC(withDebug, () -> {
            Graph graph = GSP.service(URL).defaultGraph().httpHeader("Authorization", "Bearer user:" + user).GET();
            int x = graph.size();
            assertEquals(expectedCount, x);
            return graph;
        });
    }

    private void queryWithHttpClient(WithDebug debug, String url, HttpClient httpClient, String user, String queryString, int expectedCount) {
        boolean withDebug = (debug == WithDebug.DEBUG);
        PBACTestRunner.debugPBAC(withDebug, ()->{
        // == PBAC Query
        RowSet rowSet =
                QueryExecHTTPBuilder.service(url)
                .httpClient(httpClient)
                .query(queryString)
                .select();
        long x = RowSetOps.count(rowSet);
        assertEquals(expectedCount, x);
        });
    }
}
