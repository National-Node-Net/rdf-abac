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

import uk.gov.dbt.ndtp.jena.abac.SysABAC;
import uk.gov.dbt.ndtp.jena.abac.lib.DatasetGraphABAC;
import uk.gov.dbt.ndtp.jena.abac.lib.VocabAuthz;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.fuseki.servlets.ActionErrorException;
import org.apache.jena.fuseki.servlets.ActionService;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.ServletOps;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.web.HttpSC;

/**
 * Loader for data with {@code Security-Label} header
 *  or as a TriG with graph {@code authz:labels}
 * which is {@code http://ndtp.co.uk/security#labels}
 * and constant {@link VocabAuthz#graphForLabels}).
 */
public class ABAC_DataLoader extends ActionService implements ABAC_Processor {

    @Override
    public void execPost(HttpAction action) { executeLifecycle(action); }

    @Override
    public void validate(HttpAction action) {
        // No query string
        // Recognized Content-type.
        String ct = action.getRequestContentType();
        if ( ct == null ) {
            FmtLog.warn(action.log, "[%d] No Content-Type", action.id);
            ServletOps.error(HttpSC.BAD_REQUEST_400, "No Content-Type");
        }
        Lang lang = RDFLanguages.contentTypeToLang(ct);
        if ( lang == null ) {
            FmtLog.warn(action.log, "[%d] Bad Content-Type: %s", action.id, ct);
            ServletOps.error(HttpSC.UNSUPPORTED_MEDIA_TYPE_415, "Bad Content-Type: "+ct);
        }

        if ( Lang.TRIG.equals(lang) || Lang.NQUADS.equals(lang) ) {
            // OK
        } else if ( RDFLanguages.isTriples(lang) ) {
            // Must have security label.
            boolean isLabelled = action.getRequestHeader(SysABAC.H_SECURITY_LABEL) != null;
            if ( ! isLabelled ) {
                FmtLog.warn(action.log, "[%d] Missing \"Security-Label\" header when loading triples)", action.id);
                //ServletOps.error(HttpSC.BAD_REQUEST_400, "No Security-Label header");
            }
        } else {
            // Not a supported language.
            FmtLog.warn(action.log, "[%d] Bad Content-Type: %s", action.id, ct);
            ServletOps.error(HttpSC.UNSUPPORTED_MEDIA_TYPE_415, "Bad Content-Type: "+ct);
        }

        DatasetGraph dsg =  action.getDataset();
        if ( ! ( dsg instanceof DatasetGraphABAC ) ) {
            FmtLog.warn(action.log, "[%d] Dataset '%s' does not support ABAC security labelling.", action.id, action.getDatasetName());
            ServletOps.error(HttpSC.BAD_REQUEST_400, "This dataset does not support ABAC security labelling.");
            return;
        }
    }

    @Override
    public void execute(HttpAction action) {
        try {
            LabelledDataLoader.execute(action);
        } catch (ActionErrorException ex) {
            action.abortSilent();
            throw ex;
        } catch (Throwable ex) {
            action.abortSilent();
            ServletOps.errorOccurred(ex);
            return;
        }
    }
}
