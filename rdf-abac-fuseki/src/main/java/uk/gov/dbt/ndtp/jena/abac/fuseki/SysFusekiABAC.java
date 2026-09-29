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


package uk.gov.dbt.ndtp.jena.abac.fuseki;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import uk.gov.dbt.ndtp.jena.abac.fuseki.ServerABAC.Vocab;
import org.apache.jena.fuseki.server.OperationRegistry;
import org.apache.jena.fuseki.servlets.ActionService;
import org.apache.jena.riot.WebContent;

public class SysFusekiABAC {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    public static void init() {
        boolean initialized = INITIALIZED.getAndSet(true);
         if ( initialized )
             return;
         // Load operations and handlers for Fuseki.
         // Registration is in SysABAC.
         // Use if authz:query and authz:upload needed.
         // Normally, FMod_ABAC replaces the processors for the regular query and upload operations,
         // fuseki:query, fuseki:upload (for ABAC_ChangeDispatch)

         ActionService queryLabelsProc = new ABAC_SPARQL_QueryDataset(ServerABAC.userForRequest());
         ActionService gspReadLabelsProc = new ABAC_GSP_R(ServerABAC.userForRequest());
         ActionService loaderLabelsProc = new ABAC_ChangeDispatch();
         ActionService labelsGetterProc = new ABAC_Labels();

         OperationRegistry operationRegistry = OperationRegistry.get();

         operationRegistry.register(Vocab.operationGSPRLabels, gspReadLabelsProc);
         operationRegistry.register(Vocab.operationUploadABAC, loaderLabelsProc);
         operationRegistry.register(Vocab.operationQueryLabels, queryLabelsProc);
         operationRegistry.register(Vocab.operationGetLabels, labelsGetterProc);
    }
}
