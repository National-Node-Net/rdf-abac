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

import java.util.function.Function;

import uk.gov.dbt.ndtp.jena.pbac.PBAC;
import uk.gov.dbt.ndtp.jena.pbac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.pbac.attributes.AttributeExpr;
import uk.gov.dbt.ndtp.jena.pbac.lib.CxtPBAC;
import uk.gov.dbt.ndtp.jena.pbac.lib.DatasetGraphPBAC;
import uk.gov.dbt.ndtp.jena.pbac.lib.HierarchyGetter;
import uk.gov.dbt.ndtp.jena.pbac.lib.Track;
import org.apache.jena.atlas.lib.Lib;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.ServletOps;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.web.HttpSC;
import uk.gov.dbt.ndtp.jena.pbac.opa.DecisionServiceUnavailableException;
import uk.gov.dbt.ndtp.jena.pbac.opa.PolicyDeniedException;

/**
 * Functions for any PBAC-aware operation.
 */
public class PBAC_Request {


    /**
     * Attribute name used by the PIP to carry a user's organisation, per the flat
     * attribute-string convention (see AE.parseExpr) - confirmed against ianode-access
     * swagger.json: the "deployed_organisation" user attribute is serialised into this
     * label format.
     */
    private static final String ATTR_PERMITTED_ORGANISATIONS = "permitted_organisations";

    /**
     * Provide the dataset suitable for this operation.
     * <p>
     * If this operation is not against an PBAC dataset, there is no user available, or the user is forbidden to access
     * this dataset then a suitable error is thrown which should abort further processing of the request.
     * </p>
     */
    public static DatasetGraph decideDataset(HttpAction action, DatasetGraph requestDSG, Function<HttpAction, String> getUser) {
        if ( ! ( requestDSG instanceof DatasetGraphPBAC ) ) {
            String msg = String.format("%s : Wrong type of dataset for PBAC query: %s", action.getDatasetName(), requestDSG.getClass().getSimpleName());
            reject(action, HttpSC.BAD_REQUEST_400, msg);
        }

        DatasetGraphPBAC dsgz = (DatasetGraphPBAC)requestDSG;

        String requestUser = getUser.apply(action);
        if ( requestUser == null )
            reject(action, HttpSC.BAD_REQUEST_400, "No user");
        //FmtLog.info(action.log, "[%d] User %s", action.id, requestUser);

        AttributeValueSet attributes = dsgz.attributesForUser().apply(requestUser);
        if ( attributes == null )
            reject(action, HttpSC.FORBIDDEN_403, "No request attributes for user = "+requestUser);

        HierarchyGetter function = (a)->dsgz.attributesStore().getHierarchy(a);

        CxtPBAC cxt = CxtPBAC.context(attributes, function, dsgz);
        cxt.subjectId(requestUser);
        cxt.datasetName(action.getDatasetName());
        cxt.action(action.getEndpoint() != null && action.getEndpoint().getOperation() != null
                ? action.getEndpoint().getOperation().getName()
                : "unknown");
        cxt.organisationId(extractOrganisationId(attributes));
        // TODO: fail-closed for missing organisationId (confirmed 403 by Jennifer) - NOT YET
        // ENABLED. Enforcing this broke 18 existing tests, whose test fixtures don't carry an
        // organisation attribute at all. Needs either: (a) test fixtures updated to include
        // permitted_organisations, or (b) confirmation this should only apply once SAG-03 is
        // fully wired end-to-end, not to every existing PBAC dataset unconditionally.
        FmtLog.info(action.log, "[%d] User %s : %s", action.id, requestUser, attributes);

        if ( Lib.equalsIgnoreCase("true", action.getRequestParameter("debug")) )
            cxt.tracking(Track.DEBUG);

        AttributeExpr accessAttributes = dsgz.getAccessAttributes();
        if ( accessAttributes != null && ! accessAttributes.eval(cxt).getBoolean() )
            reject(action, HttpSC.FORBIDDEN_403, "Access for user = "+requestUser);

        try {
            return PBAC.filterDataset(dsgz, cxt);
        } catch (DecisionServiceUnavailableException ex) {
            reject(action, HttpSC.SERVICE_UNAVAILABLE_503, "Policy service unavailable: " + ex.getMessage());
            return null; // unreachable - reject() does not return
        } catch (PolicyDeniedException ex) {
            reject(action, HttpSC.FORBIDDEN_403, "Access denied for user = " + requestUser);
            return null; // unreachable - reject() does not return
        }
    }

    /**
     * Reject request, with logging of the reason.
     * This function throws an exception and does not return normally.
     */
    /*package*/ static void reject(HttpAction action, int statusCode, String errorMessage) {
        // Does not return.
        FmtLog.warn(action.log, "[%d] Rejected: %s", action.id, errorMessage);
        ServletOps.error(statusCode, errorMessage);
    }

    static String extractOrganisationId(AttributeValueSet attributes) {
        String[] found = new String[1];
        attributes.attributeValues(av -> {
            if ( ATTR_PERMITTED_ORGANISATIONS.equals(av.attribute().name()) )
                found[0] = av.value().asString();
        });
        return found[0];
    }
}
