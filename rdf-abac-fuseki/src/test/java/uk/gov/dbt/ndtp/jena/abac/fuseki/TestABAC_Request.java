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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Set;
import org.apache.jena.fuseki.servlets.ActionErrorException;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.web.HttpSC;
import org.junit.jupiter.api.Test;
import uk.gov.dbt.ndtp.jena.abac.ABAC;
import uk.gov.dbt.ndtp.jena.abac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.abac.Hierarchy;
import uk.gov.dbt.ndtp.jena.abac.attributes.Attribute;
import uk.gov.dbt.ndtp.jena.abac.labels.Labels;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.abac.lib.AttributesStore;
import uk.gov.dbt.ndtp.jena.abac.lib.CxtABAC;
import uk.gov.dbt.ndtp.jena.abac.lib.DatasetFilterProvider;
import uk.gov.dbt.ndtp.jena.abac.lib.DatasetGraphABAC;
import uk.gov.dbt.ndtp.jena.abac.opa.DecisionServiceUnavailableException;
import uk.gov.dbt.ndtp.jena.abac.opa.PolicyDeniedException;

/**
 * SAG-05: verifies that a policy denial and a policy-service failure are rejected
 * before the query executes, and map to distinct, correct HTTP status codes.
 */
public class TestABAC_Request {

    /** Minimal AttributesStore: recognises exactly one user, with empty attributes. */
    private static class SingleUserStore implements AttributesStore {
        private final String user;
        SingleUserStore(String user) { this.user = user; }

        @Override
        public AttributeValueSet attributes(String u) {
            return u.equals(user) ? AttributeValueSet.of(java.util.List.of()) : null;
        }

        @Override
        public Set<String> users() { return Set.of(user); }

        @Override
        public boolean hasHierarchy(Attribute attribute) { return false; }

        @Override
        public Hierarchy getHierarchy(Attribute attribute) { return null; }
    }

    private static DatasetGraphABAC authzDatasetWithUser(String user, DatasetFilterProvider provider) {
        LabelsStore store = Labels.createLabelsStoreMem();
        DatasetGraph base = DatasetGraphFactory.createTxnMem();
        DatasetGraphABAC dsgz = ABAC.authzDataset(base, store, "dflt", new SingleUserStore(user));
        dsgz.setFilterProvider(provider);
        return dsgz;
    }

    private static HttpAction mockAction() throws Exception {
        HttpAction action = mock(HttpAction.class);
        java.lang.reflect.Field logField = HttpAction.class.getField("log");
        logField.setAccessible(true);
        logField.set(action, org.slf4j.LoggerFactory.getLogger("test"));

        when(action.getDatasetName()).thenReturn("/ds");
        when(action.getEndpoint()).thenReturn(null);
        when(action.getRequestParameter("debug")).thenReturn(null);
        return action;
    }

    @Test
    void decideDataset_opaUnavailable_rejectsWith503() throws Exception {
        DatasetFilterProvider throwing = new DatasetFilterProvider() {
            @Override
            public DatasetGraph filterDataset(DatasetGraphABAC dsgAuthz, CxtABAC cxt) {
                throw new DecisionServiceUnavailableException("OPA unreachable");
            }
            @Override
            public DatasetGraph filterDataset(DatasetGraph dsgBase, LabelsStore labels, String defaultLabel, CxtABAC cxt) {
                throw new DecisionServiceUnavailableException("OPA unreachable");
            }
        };
        DatasetGraphABAC dsgz = authzDatasetWithUser("alice", throwing);
        HttpAction action = mockAction();

        ActionErrorException ex = assertThrows(ActionErrorException.class,
                () -> ABAC_Request.decideDataset(action, dsgz, a -> "alice"));
        assertEquals(HttpSC.SERVICE_UNAVAILABLE_503, ex.getRC());    }

    @Test
    void decideDataset_policyDenied_rejectsWith403() throws Exception {
        DatasetFilterProvider denying = new DatasetFilterProvider() {
            @Override
            public DatasetGraph filterDataset(DatasetGraphABAC dsgAuthz, CxtABAC cxt) {
                throw new PolicyDeniedException("No permitted labels");
            }
            @Override
            public DatasetGraph filterDataset(DatasetGraph dsgBase, LabelsStore labels, String defaultLabel, CxtABAC cxt) {
                throw new PolicyDeniedException("No permitted labels");
            }
        };
        DatasetGraphABAC dsgz = authzDatasetWithUser("bob", denying);
        HttpAction action = mockAction();

        ActionErrorException ex = assertThrows(ActionErrorException.class,
                () -> ABAC_Request.decideDataset(action, dsgz, a -> "bob"));
        assertEquals(HttpSC.FORBIDDEN_403, ex.getRC());    }
}
