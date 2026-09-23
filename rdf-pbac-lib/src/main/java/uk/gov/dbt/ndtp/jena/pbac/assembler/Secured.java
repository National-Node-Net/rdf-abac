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


package uk.gov.dbt.ndtp.jena.pbac.assembler;

import static org.apache.jena.sparql.util.graph.GraphUtils.getStringValue;

import java.util.Set;
import java.util.function.Predicate;

import uk.gov.dbt.ndtp.jena.pbac.PBAC;
import uk.gov.dbt.ndtp.jena.pbac.lib.AttributesStore;
import uk.gov.dbt.ndtp.jena.pbac.lib.DatasetGraphPBAC;
import uk.gov.dbt.ndtp.jena.pbac.labels.Labels;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStoreMem;
import org.apache.jena.assembler.exceptions.AssemblerException;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.mem.DatasetGraphInMemory;
import org.apache.jena.sparql.util.NotUniqueException;
import org.slf4j.Logger;
import uk.gov.dbt.ndtp.jena.pbac.lib.VocabAuthzDataset;

/** Functions for building PBAC datasets and the infrastructure around them. */
public class Secured {

    /*package*/ public static Logger BUILD_LOG = PBAC.AzLOG;

    /**
     * Build a DatasetGraphAuthz from configuration and a base dataset.
     * Return null if no configuration.
     */
    public static DatasetGraphPBAC buildDatasetGraphAuthz(DatasetGraph base, Resource assemblerRoot) {
        String accessAttributes = getAccessAttributes(assemblerRoot);

        Resource labelsStoreRoot = findLabelsStore(assemblerRoot);
        LabelsStore labels = null;
        if ( labelsStoreRoot != null )
            labels = LabelStoreAssembler.labelsStore(labelsStoreRoot, assemblerRoot);

        checkStorageConfiguration(base, labels);

        if ( labels == null )
            // In-memory default.
            labels = Labels.createLabelsStoreMem();

        Resource attributesStoreRoot = findAttributesStore(assemblerRoot);
        if ( attributesStoreRoot == null )
            throw new AssemblerException(assemblerRoot, "No attribute store definition found");

        String tripleDefaultLabel = AttributeStoreBuildLib.getTripleDefaultLabel(attributesStoreRoot);
        AttributesStore attributesStore = AttributeStoreBuildLib.attributesStore(attributesStoreRoot);

        DatasetGraphPBAC dsgAuthz = PBAC.authzDataset(base, accessAttributes, labels, tripleDefaultLabel, attributesStore);
        return dsgAuthz;
    }

    /**
     * Check consistency of database and labels storage.
     * Examine the base storage and check configuration consistency.
     */
    private static void checkStorageConfiguration(DatasetGraph base, LabelsStore labels) {
        if ( base instanceof DatasetGraphInMemory ) {
            // In-memory data => in-memory labels store.
            if ( labels == null ) {
                // If null, then put in some kind of label store so new data can be loaded.
                labels = Labels.createLabelsStoreMem();
            } else {
                if ( ! ( labels instanceof LabelsStoreMem) )
                    FmtLog.warn(BUILD_LOG, "LabelsStore[%s] provided for in-memory database",
                                labels.getClass().getSimpleName() );
            }
        } else if ( org.apache.jena.tdb2.sys.TDBInternal.isTDB2(base) ) {
            // Persistent database -> labelStore required.
            if ( labels == null )
                FmtLog.error(BUILD_LOG, "No labelsStore provided for TDB2 persistent database");
        } else if ( org.apache.jena.tdb1.sys.TDBInternal.isTDB1(base) ) {
            if ( labels == null )
                FmtLog.error(BUILD_LOG, "No labelsStore provided for TDB1 persistent database");
        } else {
            FmtLog.error(BUILD_LOG, "No labelsStore provided for persistent database");
        }
    }

    private static Set<Property> inlineLabelsStoreProperties = Set.of(VocabAuthzDataset.pLabels, VocabAuthzDataset.pLabelsStorePath);
    /**
     * Get the resource for the labels store. This is either the dataset
     * or linked by {@code authz:labelsStore}.
     */
    private static Resource findLabelsStore(Resource assemblerRoot) {
        Predicate<Resource> isInline = r -> hasPropertyOneOf(r, inlineLabelsStoreProperties);
        Resource labelStoreRoot = maybeLinked(assemblerRoot, VocabAuthzDataset.pLabelsStore, isInline);
        if ( labelStoreRoot != null && labelStoreRoot != assemblerRoot ) {
            if ( ! hasProperties(labelStoreRoot) )
                throw new AssemblerException(assemblerRoot, "Empty label store description '"+ VocabAuthzDataset.pLabelsStore);
        }
        return labelStoreRoot;
    }

    private static Set<Property> inlineAttributeStoreProperties = Set.of(
        VocabAuthzDataset.pAttributes, VocabAuthzDataset.pAttributesURL);
    /**
     * Get the resource for the attributes store. This is either the dataset
     * or linked by {@code authz:attributesStore}.
     */
    private static Resource findAttributesStore(Resource root) {
        Predicate<Resource> isInline = r -> hasPropertyOneOf(r, inlineAttributeStoreProperties);
        return maybeLinked(root, VocabAuthzDataset.pAttributesStore, isInline);
    }

    /**
     * Definitions can be in simple form, where there is a property on the dataset
     * description, or in a linked resource, where this is property
     * to another resource which can have multiple properties describing the sub-unit
     * (e.g. label store or attribute store). There must be only one form; inline and
     * a linking property is illegal.
     * <p>
     * Inline is "legacy" and the preferred form is as a linked resource.
     * <pre>
     *  :dataset rdf:type authz:DatasetAuthz ;
     *      authz:labels <file:labelsInFile.ttl>
     * </pre>
     * or
     * <pre>
     *  :dataset rdf:type authz:DatasetAuthz ;
     *      authz:labelsStore [ authz:labels <file:labelsInFile.ttl> ]
     * </pre>
     * <p>
     * This function determines the root resource for the sub-unit (the subject of
     * {@code authz:labels} in the example above.
     */
    private static Resource maybeLinked(Resource root, Property linkingProperty, Predicate<Resource> isInline) {
        boolean hasLink = hasOneProperty(root, linkingProperty);
        boolean hasInline = isInline.test(root);
        if ( hasLink && hasInline )
            throw new AssemblerException(root, "Both property '"+linkingProperty+" and also inline defintion. Must be one or the other.");
        if ( !hasLink && !hasInline )
            // Nothing.
            return null;
        if ( hasInline )
            return root;
        // hasLink
        Statement s = root.getProperty(linkingProperty);
        if ( ! s.getObject().isResource() )
            throw new AssemblerException(root, "Property '"+linkingProperty+"' must have a resource for its object");
        Resource r = s.getObject().asResource();
        return r;
    }

    /** Test for zero or one properties */
    private static boolean hasProperties(Resource resource) {
        StmtIterator sIter = resource.listProperties();
        try { return sIter.hasNext(); }
        finally { sIter.close(); }
    }

    /** Test for zero or one properties */
    private static boolean hasOneProperty(Resource resource, Property property) {
        StmtIterator sIter = resource.listProperties(property);
        try {
            if ( !sIter.hasNext() )
                return false;
            sIter.next();
            if ( !sIter.hasNext() )
                return true;
            throw new NotUniqueException(resource, property);
        }
        finally {
            sIter.close();
        }
    }

    /** Does the resource have one of the properties? */
    private static boolean hasPropertyOneOf(Resource resource, Set<Property> properties) {
        for (Property p : properties ) {
            if ( resource.hasProperty(p) )
                    return true;
        }
        return false;
    }

    /**
     * Get the optional label for the PBAC required to access this dataset.
     * This is applied during request setup. It is in API (request) security.
     */
    private static String getAccessAttributes(Resource root) {
        String accessAttributes = getStringValue(root, VocabAuthzDataset.pAccessAttributes);
        if ( accessAttributes != null && accessAttributes.isEmpty() )
            throw new AssemblerException(root, ":accessAttributes is an empty string (use \"!\" for 'deny all')");
        return accessAttributes;
    }
}
