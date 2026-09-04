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


package uk.gov.dbt.ndtp.jena.abac.fuseki;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import uk.gov.dbt.ndtp.jena.abac.ABAC;
import uk.gov.dbt.ndtp.jena.abac.lib.DatasetGraphABAC;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.fuseki.Fuseki;
import org.apache.jena.fuseki.FusekiException;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.FusekiServer.Builder;
import org.apache.jena.fuseki.main.sys.FusekiModule;
import org.apache.jena.fuseki.server.*;
import org.apache.jena.fuseki.servlets.ActionProcessor;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.core.DatasetGraph;
import org.slf4j.Logger;

/**
 * Fuseki module for ABAC. This module looks for {@link DatasetGraphABAC} and ensures
 * certain operations are the correct ones that apply ABAC security.
 * {@code Operation.Query}, {@code Operation.GSP_R}, {@code Operation.Upload}.
 * <p>
 * Data change operations -- {@code Operation.Update} and {@code Operation.Patch} --
 * generate warnings. These are best handled as a separate data service, with its own
 * API authorization setup, that modify the underlying database.
 */
public class FMod_ABAC implements FusekiModule {
    public static int LEVEL = 100;

    private final static Logger LOG = ABAC.AzLOG;

    private static void init() {
        SysFusekiABAC.init();
    }

    private final Function<HttpAction, String> getUser;

    public FMod_ABAC() {
        this(ServerABAC.userForRequest());
    }

    private FMod_ABAC(Function<HttpAction, String> getUser) {
        this.getUser = getUser;
        init();
    }

    @Override
    public String name() {
        return "RDF ABAC";
    }

    @Override
    public void prepare(FusekiServer.Builder serverBuilder, Set<String> datasetNames, Model configModel) {
        FmtLog.info(Fuseki.configLog, "ABAC Fuseki Module");

        // Operation registration needs to be in InitFusekiABAC/SysFusekiABAC
        // because parsing a config file involves checks which touchthese constants.
        // i.e. do not call "serverBuilder.registerOperation(operation, handler)" here.

        for ( String name : datasetNames ) {
            prepare1(serverBuilder, name, configModel);
        }
    }

    // Inspect the configuration.
    private void prepare1(Builder serverBuilder, String name, Model configModel) {
        if ( configModel == null )
            return;

        DatasetGraph dsg = serverBuilder.getDataset(name);
        if ( dsg == null )
            return;
        if ( dsg instanceof DatasetGraphABAC dsgz) {
            FmtLog.info(LOG, "ABAC Dataset: %s", name);
            FmtLog.info(LOG, "  Default label: %s", display(dsgz.getDefaultLabel()));
            FmtLog.info(LOG, "  Access attr  : %s", display(dsgz.getAccessAttributes()));
            FmtLog.info(LOG, "  Label vocab  : %s", displayVocabulary(dsgz));
        }
    }

    private String display(Object value) {
        if ( value != null )
            return value.toString();
        return "not set";
    }

    /** Labels seeded at startup; a store loaded by upload reports these only once data arrives. */
    private String displayVocabulary(DatasetGraphABAC dsgz) {
        try {
            return dsgz.labelVocabulary().stream().sorted().toList().toString();
        } catch (UnsupportedOperationException ex) {
            return "not enumerable ("+ex.getMessage()+")";
        }
    }

    @Override
    public void configDataAccessPoint(DataAccessPoint dap, Model configModel) {
        DatasetGraph dsg = dap.getDataService().getDataset();
        if ( ! ( dsg instanceof DatasetGraphABAC ) ) {
            // Not ABAC.
            return;
        }
        // Replace the standard query handler and upload functions for this DataService
        ActionProcessor procQuery = new ABAC_SPARQL_QueryDataset(getUser);
        ActionProcessor procGSPR = new ABAC_GSP_R(getUser);
        ActionProcessor procUpload = new ABAC_ChangeDispatch();

        // Replace standard processors with ABAC ones.
        replaceOperation(dap, Operation.Query,  procQuery);
        replaceOperation(dap, Operation.GSP_R,  procGSPR);
        replaceOperation(dap, Operation.Upload, procUpload);

        // Warn about the use of these operations.
        warnOperation(dap, Operation.Update);
        warnOperation(dap, Operation.Patch);
    }

    private void replaceOperation(DataAccessPoint dap, Operation operation, ActionProcessor proc) {
        DataService dataService = dap.getDataService();

        dataService.getEndpoints(operation).forEach(ep->{
//            Endpoint ep2 = Endpoint.create()
//                    .operation(ep.getOperation())
//                    .processor(proc)
//                    .endpointName(ep.getName())
//                    .build();
            // Mutates the endpoint.
            ep.setProcessor(proc);
        });
    }

    private void warnOperation(DataAccessPoint dap, Operation operation) {
        DataService dataService = dap.getDataService();

        dataService.getEndpoints(operation).forEach(ep->{
            FmtLog.warn(Fuseki.configLog, "Update operation for DatasetGraphABAC mnot recommended");
        });
    }

    // Reverse lookup: find the resource for the registry entry name in the configuration file.
    private Resource findDatasetResource(String name, Model configModel) {
        StmtIterator sIter = configModel.listStatements(null, FusekiVocab.pServiceName, ResourceFactory.createPlainLiteral(name));

        // If not found try by canonical name, try again non-canonical (or SPARQL query)
        if ( ! sIter.hasNext() && DataAccessPoint.isCanonical(name)) {
            String name2 = name.substring(1);
            sIter = configModel.listStatements(null, FusekiVocab.pServiceName, ResourceFactory.createPlainLiteral(name2));
        }
        try {
            if ( sIter.hasNext() ) {
                Statement s = sIter.next();
                if ( sIter.hasNext() )
                    throw new FusekiException("Multiple endpoints for name '+name+'");
                Resource subject = s.getSubject();
                return subject;
            }
            throw new FusekiException("Can't find the dataset in the configuration: '"+name+"'") ;
        } finally { sIter.close(); }
    }

    /** Check the server */
    @Override
    public void server(FusekiServer server) {
        server.getDataAccessPointRegistry().forEach((name,dap) -> {
            DatasetGraph dsg = dap.getDataService().getDataset();
             if ( dsg instanceof DatasetGraphABAC )
                 checkDataset(dap);
        });
    }

    // Check ActionProcessors are authorization ones.
    private void checkDataset(DataAccessPoint dap) {
        Collection<Endpoint> endpoints = dap.getDataService().getEndpoints();
        endpoints.forEach(ep->{
            ActionProcessor proc = ep.getProcessor();
            if ( proc != null && ! isAuthzProcessor(proc) ) {
                FmtLog.warn(Fuseki.configLog,
                             "%s: Non-authorization operation processor on DatasetGraphAuthz: class %s",
                             dap.getName(), proc.getClass().getSimpleName());
            }
        });
    }

    private boolean isAuthzProcessor(ActionProcessor proc) {
        return (proc instanceof ABAC_Processor);
    }
}
