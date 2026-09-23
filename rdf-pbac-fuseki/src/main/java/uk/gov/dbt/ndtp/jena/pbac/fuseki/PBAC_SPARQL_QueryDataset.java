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

import java.util.Objects;
import java.util.function.Function;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.fuseki.servlets.ActionService;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.SPARQL_QueryDataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.web.HttpSC;

/**
 * A Fuseki {@link ActionService} that enforces attribute checking.
 */
public class PBAC_SPARQL_QueryDataset extends SPARQL_QueryDataset implements PBAC_Processor {

    private final Function<HttpAction, String> getUser;

    public PBAC_SPARQL_QueryDataset(Function<HttpAction, String> getUser) {
        this.getUser = Objects.requireNonNull(getUser, "getUser is null");
    }

    /**
     * Decide the dataset. For PBAC, this builds the per-request dataset.
     */
    @Override
    protected Pair<DatasetGraph, Query> decideDataset(HttpAction action, Query query, String queryStringLog) {
        if ( query.hasDatasetDescription() )
            PBAC_Request.reject(action, HttpSC.BAD_REQUEST_400, "FROM/FROM NAMED in query to PBAC dataset.");

        // Provide the dataset suitable for this operation.
        DatasetGraph dsg0 = getDataset(action);
        DatasetGraph dsg = PBAC_Request.decideDataset(action, dsg0, getUser);
        return Pair.create(dsg, query);
    }

    @Override
    protected QueryExecution createQueryExecution(HttpAction action, Query query, DatasetGraph dataset) {
        return super.createQueryExecution(action, query, dataset);
    }
}
