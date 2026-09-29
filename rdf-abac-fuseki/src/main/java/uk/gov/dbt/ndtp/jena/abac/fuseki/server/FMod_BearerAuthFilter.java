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


package uk.gov.dbt.ndtp.jena.abac.fuseki.server;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import uk.gov.dbt.ndtp.jena.abac.lib.DatasetGraphABAC;
import uk.gov.dbt.ndtp.jena.abac.fuseki.FMod_ABAC;
import jakarta.servlet.Filter;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.fuseki.Fuseki;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.auth.AuthBearerFilter;
import org.apache.jena.fuseki.main.auth.BearerMode;
import org.apache.jena.fuseki.main.sys.FusekiModule;
import org.apache.jena.fuseki.server.DataAccessPoint;
import org.apache.jena.fuseki.server.DataAccessPointRegistry;
import org.apache.jena.fuseki.server.Endpoint;
import org.apache.jena.fuseki.server.Operation;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.DatasetGraph;
import org.slf4j.Logger;

/**
 * FMod responsible for inserting a servlet filter AuthBearerFilter
 * based on which endpoints and operation need it.
 * This should be placed after {@link FMod_ABAC} in the module order
 * modules nest so "after" becomes "wraps {@link FMod_ABAC}".
 */
public class FMod_BearerAuthFilter implements FusekiModule {

    private static Logger LOG = Fuseki.configLog;

    // The set of endpoints
    private final Function<DataAccessPoint, Set<String>> pathspecsFunction;
    private final Function<String, String> userFromToken;
    private final BearerMode bearerMode;
    private final Set<Operation> bearerAuthOperations;

    private FMod_BearerAuthFilter(Function<DataAccessPoint, Set<String>> pathspecs,
                                  Set<Operation> bearerAuthOperations,
                                  Function<String, String> userFromToken,
                                  BearerMode bearerMode) {
        if ( pathspecs == null && bearerAuthOperations == null )
            throw new IllegalArgumentException("Must supply exactly one of 'pathspecs' and 'bearerAuthOperations'. Both are null");
        if ( pathspecs != null && bearerAuthOperations != null )
            throw new IllegalArgumentException("Must supply exactly one of 'pathspecs' and 'bearerAuthOperations'. Both are defined");
        this.pathspecsFunction = pathspecs != null
                ? pathspecs
                : pathspecsByOperation(bearerAuthOperations);
        this.userFromToken = Objects.requireNonNull(userFromToken);
        this.bearerMode = Objects.requireNonNull(bearerMode);
        this.bearerAuthOperations = bearerAuthOperations;
    }

    public FMod_BearerAuthFilter(Function<DataAccessPoint, Set<String>> pathspecs,
                                 Function<String, String> userFromToken,
                                 BearerMode bearerMode) {
        this(pathspecs, null, userFromToken, bearerMode);
    }


    public FMod_BearerAuthFilter(Set<Operation> bearerAuthOperations,
                                 Function<String, String> userFromToken,
                                 BearerMode bearerMode) {
        this(null, bearerAuthOperations, userFromToken, bearerMode);
    }



    private static Function<DataAccessPoint, Set<String>> pathspecsByOperation(Set<Operation> bearerAuthOperations) {
        return (dap)->determineEndpoints(dap, bearerAuthOperations);
    }

    @Override
    public String name() {
        return "Bearer Authentication";
    }

    @Override
    public void prepare(FusekiServer.Builder serverBuilder, Set<String> datasetNames, Model configModel) {
        FmtLog.info(Fuseki.configLog, "BearerAuthFilter Fuseki Module");
    }

    /**
     * Walk the DataAccessPointRegistry to find all the places where a
     * a bearer auth filter is needed based on looking at the operations at every
     * endpoint.
     */
    @Override
    public void configured(FusekiServer.Builder serverBuilder, DataAccessPointRegistry dapRegistry, Model configModel) {
        for ( DataAccessPoint dap : dapRegistry.accessPoints() ) {
            Set<String> pathspecs = pathspecsFunction.apply(dap);
            if ( pathspecs == null )
                continue;
            if ( pathspecs.isEmpty() ) {
                LOG.info("No dataset paths for auth filter");
                continue;
            }
            LOG.info("Add bearer auth filter dataset paths: "+pathspecs);
            Filter servletFilter = authFilter();
            // Apply to all end points that need it bearer auth provided.
            // sorted is just for convenience.
            pathspecs.stream().sorted().forEach(pathspec->{
                serverBuilder.addFilter(pathspec, servletFilter);
            });
        }
    }

    private Filter authFilter() {
        return new AuthBearerFilter(userFromToken, bearerMode);
    }

    /**
     * Return pathspec that need a auth filter by inspecting the operation.
     * This can return null for not applicable to this DataAccessPoint
     */
    private static Set<String> determineEndpoints(DataAccessPoint dap, Set<Operation> bearerAuthOperations) {
        DatasetGraph dsg = dap.getDataService().getDataset();
        if ( ! (dsg instanceof DatasetGraphABAC) )
            return null;
        String name = dap.getName();
        String cname = DataAccessPoint.canonical(name);

        Collection<Endpoint> endpoints =  dap.getDataService().getEndpoints();

        Set<String> pathspecs = new HashSet<>();

        endpoints.forEach(ep->{
            Operation op = ep.getOperation();
            if ( bearerAuthOperations.contains(op) ) {
                String epName = ep.getName();
                String pathspec = cname;
                if ( ! epName.isEmpty() )
                    pathspec = pathspec + "/" + epName;
                pathspecs.add(pathspec);
            }
        });
        return pathspecs;
    }
}
