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


package uk.gov.dbt.ndtp.jena.pbac.evalserver;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import uk.gov.dbt.ndtp.jena.pbac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.pbac.attributes.AttributeExpr;
import uk.gov.dbt.ndtp.jena.pbac.attributes.AttributeParser;
import uk.gov.dbt.ndtp.jena.pbac.attributes.AttributeSyntaxError;
import uk.gov.dbt.ndtp.jena.pbac.attributes.ValueTerm;
import uk.gov.dbt.ndtp.jena.pbac.lib.AttributesStore;
import uk.gov.dbt.ndtp.jena.pbac.lib.CxtPBAC;
import uk.gov.dbt.ndtp.jena.pbac.services.LibAuthService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.fuseki.servlets.ActionService;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.ServletAction;
import org.apache.jena.fuseki.servlets.ServletOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributeEvalServer {

    public static final Logger LOG = LoggerFactory.getLogger("ALE");

    public static HttpServlet actionService(AttributesStore attrStore) {
        ActionService actionService = new AttributeEvalServer.AttributeLabelEvaluator(attrStore);
        return new ServletAction(new AttributeLabelEvaluator(attrStore), LOG);
    }

    public static String run(int port, String path, AttributesStore attrStore) {
        ActionService actionService = new AttributeEvalServer.AttributeLabelEvaluator(attrStore);
        String URL = LibAuthService.run(port, LOG, List.of(Pair.create(path, actionService)));
        return LibAuthService.serviceURL(URL, path);
    }

    /**
     * <pre>
     * path?user=...&label=...
     * </pre>
     */
    static class AttributeLabelEvaluator extends ActionService {

        private static final String PARAM_USER = "user";
        private static final String PARAM_LABEL = "label";
        private final AttributesStore attributesStore;

        public AttributeLabelEvaluator(AttributesStore attrStore) {
            this.attributesStore = attrStore;
        }

        // Implemented methods.
        //@Override public void execGet(HttpAction action) { executeLifecycle(action); }
        @Override public void execPost(HttpAction action) { executeLifecycle(action); }

        @Override
        public void validate(HttpAction action) {
            Map<String, String[]> m = action.getRequestParameterMap();
            if ( m.containsKey(PARAM_LABEL) && m.containsKey(PARAM_USER) )
                return;
            if ( ! m.containsKey(PARAM_LABEL) && ! m.containsKey(PARAM_USER) )
                ServletOps.errorBadRequest("No 'label' and no 'user' query parameter");
            if ( ! m.containsKey(PARAM_LABEL) )
                ServletOps.errorBadRequest("No 'label' query parameter");
            if ( ! m.containsKey(PARAM_USER) )
                ServletOps.errorBadRequest("No 'user' query parameter");
            if ( m.get(PARAM_USER).length >= 2 )
                ServletOps.errorBadRequest("More than one 'user' query parameter");
            if ( m.get(PARAM_LABEL).length >= 2 )
                ServletOps.errorBadRequest("More than one 'label' query parameter");
        }

        private static final ValueTerm dftResult = ValueTerm.FALSE;

        /** execute
         * <p>
         * request: query string:
         * </p>
         * <pre>
         *   {@code ?user=USER&label=LABEL}
         * </pre>
         * <p>
         * response:
         * <pre>
         *   400 - Bad request
         *   200 - JSON:
         *   {
         *      "user" : "..." ;
         *      "result" : "STRING"
         *   }
         * </pre>
         * <p>
         * where STRING is "true" or "false".
         * </p>
         * <p>
         * If the user is unknown, return "false"
         * </p>
         */
        @Override
        public void execute(HttpAction action) {
            // c.f. GSPLib. Move to one place!
            String user = getOneOnly(action, PARAM_USER);
            String label = getOneOnly(action, PARAM_LABEL);

            JsonObject jObj = JSON.buildObject(jb->{
                jb.pair("user", user);

                AttributeValueSet avSet = attributesStore.attributes(user);
                if ( avSet == null ) {
                    LOG.info("No attributes for user: "+user);
                    // Return false.
                    jb.pair("result", dftResult.asString());
                    return ;
                }

                // Parse - evaluate
                List<AttributeExpr> attrExprs;
                try {
                    //parseAttrExprList
                    attrExprs = AttributeParser.parseAttrExprList(label);
                } catch (AttributeSyntaxError ex) {
                    ServletOps.errorBadRequest("Bad syntax: "+ex.getMessage());
                    /*does not*/return ;
                }

                CxtPBAC context = CxtPBAC.context(avSet, attributesStore::getHierarchy, null);
                // Default of a zero length list.
                boolean allow = dftResult.getBoolean();
                for ( AttributeExpr attrExpr : attrExprs ) {
                    ValueTerm vt = attrExpr.eval(context);
                    if ( !vt.isBoolean() )
                        allow = false;
                    else
                        allow = vt.getBoolean();
                    if ( ! allow )
                        break;
                }
                ValueTerm vt = ValueTerm.value(allow);
                jb.pair("result", vt.asString());
                LOG.info("Result for user: "+user+" :: "+allow);
            });

            try( ServletOutputStream out = action.getResponse().getOutputStream() ) {
                PrintStream ps = new PrintStream(out);
                JSON.write(out, jObj);
                ServletOps.success(action);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String getOneOnly(HttpAction action, String param) {
            return action.getRequestParameter(param);
        }
    }
}
