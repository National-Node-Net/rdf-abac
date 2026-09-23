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


package uk.gov.dbt.ndtp.jena.pbac;

import uk.gov.dbt.ndtp.jena.pbac.assembler.SecuredDatasetAssembler;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AEX;
import uk.gov.dbt.ndtp.jena.pbac.lib.DatasetGraphPBAC;
import uk.gov.dbt.ndtp.jena.pbac.lib.VocabAuthzDataset;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsGetter;
import org.apache.jena.atlas.lib.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SysPBAC {

    /**
     * Security-Label : The default label that applies to a data payload.
     */
    public static final String H_SECURITY_LABEL = "Security-Label";

    /** Constant for "deny all" */
    public static final String DENY_LABEL = AEX.STR_DENY;

    /** Constant for "allow all" */
    public static final String ALLOW_LABEL = AEX.STR_ALLOW;

    /**
     * System-wide default used when there isn't an appropriate label or an error occurred.
     * <p>
     * Normally, a default attribute is associated with {@link DatasetGraphPBAC}
     * via the {@link SecuredDatasetAssembler} configuration.
     */
    public static final String SYSTEM_DEFAULT_TRIPLE_ATTRIBUTES = DENY_LABEL;

    /**
     * Result if there are no labels or label patterns configured for a dataset.
     * ({@link LabelsGetter} returns null).
     * @implNote
     * Used by {@code SecurityFilterByLabel}.
     */
    public static final boolean DEFAULT_CHOICE_NO_LABELS = true;

    public static Logger SYSTEM_LOG = LoggerFactory.getLogger("uk.gov.dbt.ndtp.jena.pbac");
    public static Logger DEBUG_LOG = LoggerFactory.getLogger("uk.gov.dbt.ndtp.pbac.SecurityFilter");

        /** The product name */
    public static final String NAME         = "RDF PBAC";

    /** Software version taken from the jar file. */
    public static final String VERSION      = Version.versionForClass(SysPBAC.class).orElse("<development>");

    public static void init() {
        VocabAuthzDataset.init();
    }
}
