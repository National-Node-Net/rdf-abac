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


package uk.gov.dbt.ndtp.jena.abac.evalserver;

import uk.gov.dbt.ndtp.jena.abac.lib.AttributeStoreCache;
import uk.gov.dbt.ndtp.jena.abac.lib.Attributes;
import uk.gov.dbt.ndtp.jena.abac.lib.AttributesStore;
import uk.gov.dbt.ndtp.jena.abac.lib.AttributesStoreRemote;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.cmd.ArgDecl;
import org.apache.jena.cmd.CmdException;
import org.apache.jena.cmd.CmdGeneral;
import org.apache.jena.fuseki.system.FusekiLogging;
import org.apache.jena.graph.Graph;
import org.apache.jena.http.HttpEnv;
import org.apache.jena.irix.IRIException;
import org.apache.jena.irix.IRIs;
import org.apache.jena.irix.IRIx;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.sys.JenaSystem;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.format.DateTimeParseException;

public class MainAttrEvalServer extends CmdGeneral {

    private static final Logger LOG = AttributeEvalServer.LOG;

    static { JenaSystem.init(); }

    public static void main(String...args) {
        new MainAttrEvalServer(args).mainRun() ;
    }

    private static AttributesStore createRemoteAttributeStore(String lookupUserEndpoint, String lookupHierarchyEndpoint) {
        return new AttributesStoreRemote(lookupUserEndpoint, lookupHierarchyEndpoint, HttpEnv.getDftHttpClient());
    }

    private static AttributesStore createLocalAttributeStore(String localAttributeStoreFile) {
        Graph g = RDFParser.source(localAttributeStoreFile).toGraph();
        AttributesStore attrStoreLocal = Attributes.buildStore(g);
        return attrStoreLocal;
    }

    private AttributesStore createCachedAttributeStore(AttributesStore attributesStore) {
        return new AttributeStoreCache(attributesStore, attributeCacheExpiry, hierarchyCacheExpiry, attributeCacheSize, hierarchyCacheSize);
    }

    private static final ArgDecl argPort      = new ArgDecl(ArgDecl.HasValue, "port");
    private static final ArgDecl argConfig    = new ArgDecl(ArgDecl.HasValue, "config", "conf");
    private static final ArgDecl argStoreURL  = new ArgDecl(ArgDecl.HasValue, "attrStore", "attrstore", "store");

    private String configFile       = null;
    private String storeURL         = null;
    private int port                = 0;
    private boolean cache           = false;
    private Duration hierarchyCacheExpiry;
    private Duration attributeCacheExpiry;
    private long hierarchyCacheSize = 5L;
    private long attributeCacheSize = 50L;
    private static final int DEFAULT_PORT = 4045;

    protected MainAttrEvalServer(String[] argv) {
        super(argv);
        add(argConfig,   "--config",      "Configuration file");
        add(argStoreURL, "--attrstore",   "File name for local attribute store or URL for remote store");
        add(argPort,     "--port",        "Listen on this port number");

        // --config conf.json.
        // OR
        // --attrstore [file:]|https://
    }

    @Override
    protected String getSummary() {
        return null;
    }

    @Override
    protected String getCommandName() {
        return null;
    }

    private String localAttributeStore = null;
    private String lookupUserEndpoint = "";
    private String lookupHierarchyEndpoint = "";

    @Override
    protected void processModulesAndArgs() {
        super.processModulesAndArgs();
        configFile = super.getValue(argConfig);
        storeURL = super.getValue(argStoreURL);

        port = portNumber(argPort);
        if ( port <=0 )
            port = DEFAULT_PORT;

        if ( configFile == null && storeURL == null)
            throw new CmdException("Required: one of --attrstore and --config");
        if ( configFile != null && storeURL != null)
            throw new CmdException("Required: one of --attrstore and --config");

        if ( storeURL != null ) {
            try {
                IRIx iri = IRIx.create(storeURL);
                if ( ! iri.isAbsolute() )
                    throw new CmdException("Bad URI for attribute store: "+storeURL);
                String scheme = IRIs.scheme(storeURL);
                if ( scheme == null || "file".equalsIgnoreCase(scheme) ) {
                    localAttributeStore = storeURL;
                    lookupUserEndpoint = null;
                    lookupHierarchyEndpoint = null;
                } else {
                    localAttributeStore = null;
                    lookupUserEndpoint = storeURL;
                    lookupHierarchyEndpoint = storeURL;
                }
            } catch (IRIException ex) {
                throw new CmdException("Bad syntax in URI for attribute store: "+storeURL);
            }
        }

        if (configFile != null) {
            readConfigFile(configFile);
        }
    }

    private void readConfigFile(String configFile) {
        if ( configFile == null  ) {
            throw new IllegalArgumentException("Required: configFile");
        }
        JsonObject jObject = JSON.read(configFile);

        if (jObject == null) {
            throw new RuntimeException("Invalid configuration file");
        }

        if (jObject.hasKey("cache") && jObject.get("cache").getAsBoolean().value()) {
            attributeCacheExpiry =
                    parseDuration(jObject.getString("attributeCacheExpiryTime"), Duration.ofSeconds(60));
            hierarchyCacheExpiry =
                    parseDuration(jObject.getString("hierarchyCacheExpiryTime"), Duration.ofMinutes(5));
            if (jObject.hasKey("attributeCacheSize"))
                attributeCacheSize = jObject.getNumber("attributeCacheSize").longValue();
            if (jObject.hasKey("hierarchyCacheSize"))
                hierarchyCacheSize = jObject.getNumber("hierarchyCacheSize").longValue();
        }
    }

    @Override
    protected void exec() {
        AttributesStore attrStore =
                (localAttributeStore != null)
                ? createLocalAttributeStore(localAttributeStore)
                : createRemoteAttributeStore(lookupUserEndpoint, lookupHierarchyEndpoint);
        if (cache) {
            attrStore = createCachedAttributeStore(attrStore);
        }
        FusekiLogging.setLogging();
        FmtLog.info(LOG, "Attribute evaluation server : port = %s", port);
        String URL = AttributeEvalServer.run(port, "/eval", attrStore);
        FmtLog.info(LOG, "URL: %s", URL);
    }

    private int portNumber(ArgDecl arg) {
        String portStr = getValue(arg);
        if ( null == portStr || portStr.isEmpty() )
            return -1;
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException ex) {
            throw new CmdException(argPort.getKeyName() + " : bad port number: '" + portStr+"'");
        }
    }

    private static Duration parseDuration(String duration, Duration defaultDuration) {
        if (null == duration || duration.isEmpty()) {
            return defaultDuration;
        }
        try {
            return Duration.parse(duration);
        } catch (DateTimeParseException ex) {
            throw new CmdException("Bad syntax in config file duration: " + duration);
        }
    }
}
