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

import org.apache.jena.fuseki.servlets.GSP_R;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.sparql.core.DatasetGraph;

public class PBAC_GSP_R extends GSP_R implements PBAC_Processor {

    private final Function<HttpAction, String> getUser;

    public PBAC_GSP_R(Function<HttpAction, String> getUser) {
        this.getUser = Objects.requireNonNull(getUser, "getUser is null");
    }

    @Override
    protected DatasetGraph decideDataset(HttpAction action) {
        DatasetGraph dsg0 = action.getActiveDSG();
        DatasetGraph dsg = PBAC_Request.decideDataset(action, dsg0, getUser);
        return dsg;
    }
}
