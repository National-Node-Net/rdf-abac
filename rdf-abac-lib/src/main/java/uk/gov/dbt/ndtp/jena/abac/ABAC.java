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


package uk.gov.dbt.ndtp.jena.abac;

import uk.gov.dbt.ndtp.jena.abac.assembler.SecuredDatasetAssembler;
import uk.gov.dbt.ndtp.jena.abac.labels.Labels;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsGetter;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStoreZero;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFilteredView;
import org.apache.jena.sparql.graph.GraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import uk.gov.dbt.ndtp.jena.abac.lib.*;

/**
 * Programmatic API to the Attribute-Based Access Control functionality.
 *
 * @see SecuredDatasetAssembler
 */
public final class ABAC {

    private ABAC() {
    }

    public final static Logger AzLOG = LoggerFactory.getLogger("uk.gov.dbt.ndtp.jena.abac.Authz");
    public final static Logger AttrLOG = LoggerFactory.getLogger("uk.gov.dbt.ndtp.jena.abac.Attribute");

    /**
     * Operate with old style attributes only.
     * This applies to labels, attribute store description, and remote attribute store.
     * No value, no quotes, any character set - no white space or =
     */
    public static boolean LEGACY = true;

    /**
     * Per request label evaluation cache size.
     */
    public static final int LABEL_EVAL_CACHE_SIZE = 100_000;

    /**
     * Per request hierarchy retrieval cache size.
     * This could become a global cache. The answers are not request sensitive.
     */
    public static final int HIERARCHY_CACHE_SIZE = 100;

    /**
     * Test whether a dataset supports ABAC data labelling.
     */
    public static boolean isDatasetABAC(DatasetGraph dsg) {
        return dsg instanceof DatasetGraphABAC;
    }

    /**
     * Create a {@link DatasetGraphABAC}.
     */
    public static DatasetGraphABAC authzDataset(DatasetGraph dsgBase, LabelsStore labels, String datasetDefaultLabel, AttributesStore attributesStore) {
        return authzDataset(dsgBase, null, labels, datasetDefaultLabel, attributesStore);
    }

    /**
     * Create a {@link DatasetGraphABAC}.
     */
    public static DatasetGraphABAC authzDataset(DatasetGraph dsgBase, String accessAttributes,
                                                LabelsStore labels, String datasetDefaultLabel,
                                                AttributesStore attributesStore) {
        return new DatasetGraphABAC(dsgBase, accessAttributes, labels, datasetDefaultLabel, attributesStore);
    }

    /**
     * Build an attribute-enforcing DatasetGraph. For programmatic/API use,
     * and in unit tests.
     * <p>
     * This is not used by Fuseki.
     */
    public static DatasetGraph requestDataset(DatasetGraphABAC dsgAuthz, AttributeValueSet attributes, HierarchyGetter function) {
        CxtABAC cxt = CxtABAC.context(attributes, function, dsgAuthz.getData());
        return filterDataset(dsgAuthz, cxt);
    }

    /**
     * Build an attribute-enforcing DatasetGraph. For programmatic/API use,
     * and in unit tests.
     * <p>
     * This is not used by Fuseki.
     */
    public static DatasetGraph requestDataset(DatasetGraphABAC dsgAuthz, AttributeValueSet attributes, AttributesStore attrStore) {
        CxtABAC cxt = CxtABAC.context(attributes, attrStore, dsgAuthz.getData());
        return filterDataset(dsgAuthz, cxt);
    }

    /**
     * Create an attribute-filtered dataset for a context.
     *
     * @see #requestDataset
     */
    public static DatasetGraph filterDataset(DatasetGraphABAC dsgAuthz, CxtABAC cxt) {
        return ABACRequest.resolveProvider(dsgAuthz).filterDataset(dsgAuthz, cxt);
    }

    /**
     * Create an attribute-filtered dataset for a context.
     * <p>
     * "No label store" (null) is not the same as an empty label store
     * ({@link LabelsStoreZero}). "No store" mean the label filter isn't even
     * incorporated into decisions whereas an empty store may have a default.
     * <p>The DatasetGraph is the data storage dataset.
     * <p>
     * No {@link DatasetGraphABAC} is available here, so only the global provider
     * applies (no per-dataset override lookup possible) - matches upstream's
     * equivalent call site.
     */
    public static DatasetGraph filterDataset(DatasetGraph dsgBase, LabelsStore labels, String defaultLabel, CxtABAC cxt) {
        return ABACRequest.getFilterProvider().filterDataset(dsgBase, labels, defaultLabel, cxt);
    }

    /**
     * Read SHACL from a classpath resource or file path.
     */
    public static Shapes readSHACL(String resource) {
        Graph gShacl = GraphFactory.createDefaultGraph();
        Shapes shapes = tryClassPath(resource, gShacl);
        if (shapes == null) {
            return tryFilePath(resource, gShacl);
        } else {
            return shapes;
        }
    }

    private static Shapes tryClassPath(String resource, Graph gShacl) {
        try (InputStream in = ABAC.class.getClassLoader().getResourceAsStream(resource)) {
            if (in != null) {
                RDFParser.source(in).lang(Lang.SHACLC).parse(gShacl);
                return ShaclValidator.get().parse(gShacl);
            } else {
                return null;
            }
        } catch (IOException ioex) {
            return null;
        }
    }

    private static Shapes tryFilePath(String resource, Graph gShacl) {
        try (InputStream in = new FileInputStream(resource)) {
            RDFParser.source(in).lang(Lang.SHACLC).parse(gShacl);
            return ShaclValidator.get().parse(gShacl);
        } catch (IOException fnfex) {
            return null;
        }
    }

}
