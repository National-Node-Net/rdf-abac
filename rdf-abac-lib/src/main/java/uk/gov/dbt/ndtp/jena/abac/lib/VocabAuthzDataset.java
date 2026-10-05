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


package uk.gov.dbt.ndtp.jena.abac.lib;

import uk.gov.dbt.ndtp.jena.abac.assembler.SecuredDatasetAssembler;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.assembler.AssemblerUtils;

import java.time.Duration;

/**
 * Vocabulary for assembler for authz datasets.
 */
public class VocabAuthzDataset {

    private static final String NS = VocabAuthz.getURI();
    public static String getURI() {
        return VocabAuthz.getURI();
    }

    public static Resource tDatasetAuthz = ResourceFactory.createResource(NS+"DatasetAuthz");

    /** The underlying dataset being wrapped in authorization. */
    public static Property pDataset = ResourceFactory.createProperty(NS+"dataset");

    /**
     * Attributes required for accessing the data.
     * Failure to match will return a 403.
     */
    public static Property pAccessAttributes = ResourceFactory.createProperty(NS+"accessAttributes");

    // ---- Attributes Store
    //   One of pAttributes, pAttributesStoreURL
    //        pAttributes - local file
    //        pAttributesURL - URL to an external service

    //   Optional pHierarchiesURL

    //   One of pLabels, pLabelsStore
    //     If pLabelsStore, then [ pLabelsStorePath .... ]

    //   Optional pTripleDefaultLabels

    /**
     * Property to refer to a resource for the definition of the attribute store.
     * An alternative to defining on the DatasetGraphABAC assembler.
     */
    public static Property pAttributesStore = ResourceFactory.createProperty(NS+"attributesStore");

    /**
     * Attribute Store: either a file name, which is read into an in-memory or a
     * dataset in this assembler where the default graph holds the attributes
     * configuration.
     */
    public static Property pAttributes = ResourceFactory.createProperty(NS+"attributes");

    /**
     * Attribute Store: Remote store - URL of the user attribute lookup service.
     */
    public static Property pAttributesURL = ResourceFactory.createProperty(NS+"attributesURL");

    /**
     * Attribute Store: Remote store - URL of the hierarchy lookup service.
     */
    public static Property pHierarchiesURL = ResourceFactory.createProperty(NS+"hierarchiesURL");

    /**
     * Cached Attribute Store:
     */
    public static final Property pCachedStore = ResourceFactory.createProperty(NS + "cache");
    public static final Property pAttributeCacheExpiry = ResourceFactory.createProperty(NS + "attributeCacheExpiryTime");
    public static final Property pHierarchyCacheExpiry = ResourceFactory.createProperty(NS + "hierarchyCacheExpiryTime");
    public static final Duration defaultHierarchyCacheExpiry = Duration.ofMinutes(10);
    public static final Duration defaultAttributeCacheExpiry = Duration.ofSeconds(30);
    public static final Property pAttributeCacheSize = ResourceFactory.createProperty(NS + "attributeCacheSize");
    public static final Property pHierarchyCacheSize = ResourceFactory.createProperty(NS + "hierarchyCachedSize");
    public static final long DEFAULT_HIERARCHY_CACHE_SIZE = 5L;
    public static final long DEFAULT_ATTRIBUTE_CACHE_SIZE = 50L;
    // ---- Labels Store

    /**
     * Property to refer to a labels store definition
     * (if not part of the authz:DatasetAuthz).
     */
    public static Property pLabelsStore = ResourceFactory.createProperty(NS+"labelsStore");

    /**
     * Labels: Direct link to a file name, which is read into an in-memory labels data-structure.
     */
    public static Property pLabels = ResourceFactory.createProperty(NS+"labels");

    // RocksDB-based label store.
    /**
     * Property to refer to directory for the RocksDB database.
     */
    public static Property pLabelsStorePath = ResourceFactory.createProperty(NS+"labelsStorePath");
    /**
     * Property to refer to set-up of RocksDB database
     */
    public static Property pLabelsStoreByHash = ResourceFactory.createProperty(NS+"labelsStoreByHash");
    public static Property pLabelsStoreByString = ResourceFactory.createProperty(NS+"labelsStoreByString");
    public static Property pLabelsStoreUpdateModeOverwrite = ResourceFactory.createProperty(NS+"labelsStoreUpdateModeOverwrite");
    public static Property pLabelsStoreUpdateModeMerge = ResourceFactory.createProperty(NS+"labelsStoreUpdateModeMerge");
    public static Property pLabelsStoreByteBufferSize = ResourceFactory.createProperty(NS+"labelsStoreByteBufferSize");
    public static Property pLabelsStoreByHashFunction = ResourceFactory.createProperty(NS+"labelsStoreByHashFunction");

    // -- Dataset attribute settings.

    /**
     * Attributes for when a triple has no matching attribute expression.
     * Failure to match will hide the triple.
     */
    public static Property pTripleDefaultLabels = ResourceFactory.createProperty(NS+"tripleDefaultLabels");

    /** @deprecated Use {@link #pTripleDefaultLabels} */
    @Deprecated
    public static Property pTripleDefaultAttributes = ResourceFactory.createProperty(NS+"tripleDefaultAttributes");

    private static boolean initialized = false;

    static { init() ; }

    static synchronized public void init() {
        if ( initialized )
            return;
        initialized = true;
        AssemblerUtils.registerDataset(tDatasetAuthz, new SecuredDatasetAssembler());
    }
}
