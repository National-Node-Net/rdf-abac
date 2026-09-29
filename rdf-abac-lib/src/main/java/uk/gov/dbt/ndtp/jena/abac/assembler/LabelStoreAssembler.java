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


package uk.gov.dbt.ndtp.jena.abac.assembler;

import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStoreRocksDB.LabelMode;
import uk.gov.dbt.ndtp.jena.abac.labels.StoreFmtByHash;
import uk.gov.dbt.ndtp.jena.abac.labels.StoreFmtByString;
import uk.gov.dbt.ndtp.jena.abac.labels.hashing.Hasher;
import uk.gov.dbt.ndtp.jena.abac.labels.hashing.HasherUtil;
import org.apache.jena.assembler.exceptions.AssemblerException;
import org.apache.jena.atlas.lib.IRILib;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.assembler.InMemDatasetAssembler;
import org.apache.jena.sparql.util.graph.GraphUtils;
import org.apache.jena.tdb2.assembler.VocabTDB2;
import org.apache.jena.vocabulary.RDF;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import uk.gov.dbt.ndtp.jena.abac.lib.VocabAuthzDataset;
import uk.gov.dbt.ndtp.jena.abac.labels.Labels;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.abac.labels.StoreFmt;

/**
 * Assembler for a label store based on a stored graph.
 * This was used up to RDF ABAC 0.20.0
 */
public class LabelStoreAssembler {

    /*
     *  []
     *      authz:labels         :databaseLabels ;
     *
     * or in-memory (file is a graph of labels in "published" format)
     *      authz:labels          <file:path/labels.ttl>
     * or
     *      authz:labels          "file:path/labels.ttl"
     *
     * or RocksDB store
     *      authz:labelsStorePath <file:directory> ] ;
     */

    /*package*/ static LabelsStore labelsStore(Resource labelsStoreRoot, Resource assemblerRoot) {
        try {
            // RocksDB
            if ( isRocksDBLabelsStore(labelsStoreRoot) ) {
                boolean inlineLabelsStoreDefn = (labelsStoreRoot == assemblerRoot);
                if ( ! inlineLabelsStoreDefn ) {
                    // Scope for later tuning configuration for RocksDB
                    // Other configuration, if it is
                    //   "authz:labelsStore  [ authz:labelslStorePath <file:directory> ]"
                    // and not
                    //   "authz:labelslStorePath <file:directory>"
                    // on the DatasetAuthz
                }
                return createLabelStoreRocksDB(labelsStoreRoot);
            }

            // Not Rocks.
            RDFNode obj = GraphUtils.getAsRDFNode(labelsStoreRoot, VocabAuthzDataset.pLabels);
            if ( obj == null )
                return null;

            if ( obj.isLiteral() ) {
                FmtLog.error(Secured.BUILD_LOG, "authz:labels must refer to a file and not a literal.");
                return null;
            }

            if ( ! subjectInThisAssembler(obj) ) {
                // Not another resource in the configuration file.
                //  authz:labels          <file:path/labels.ttl>
                //  authz:labels          "file:path/labels.ttl"
                // Treat as a file name.
                return labelsFile(labelsStoreRoot);
            }

            // Subject in this Assembler. This used to be a dataset where labels could be persisted in the graph format.
            // Superseded by RocksDB labels store.
            boolean loggedMessage = warningLegacyUseOfAuthzLabels(obj.asResource());
            if ( loggedMessage )
                FmtLog.error(Secured.BUILD_LOG, "authz:labels must refer to a file and not a resource in the configuration file.");
            return null;

        } catch ( AssemblerException ex ) {
            throw ex;
        } catch (Throwable th) {
            // Something went wrong.
            //FmtLog.error(Secured.BUILD_LOG, "Failed to build the labels store: "+assemblerRoot, th);
            throw new AssemblerException(assemblerRoot, "Failed to build the labels store: "+assemblerRoot, th);
        }
    }

    // Subject in this Assembler. This used to be a dataset where labels could be persisted in the graph format.
    private static boolean warningLegacyUseOfAuthzLabels(Resource r) {
        StmtIterator types = r.listProperties(RDF.type);
        boolean loggedMessage = false;
        if ( r.hasProperty(RDF.type, InMemDatasetAssembler.getType()) ) {
            loggedMessage = true;
            FmtLog.warn(Secured.BUILD_LOG, "In-memory dataset for authz:labels. Ignored");
        }
        if ( r.hasProperty(RDF.type, VocabTDB2.tDatasetTDB) ) {
            loggedMessage = true;
            FmtLog.error(Secured.BUILD_LOG, "TDB2 dataset for authz:labels. Not supported");
        }
        return loggedMessage;
    }

    /**
     * Create a label store in-memory, initialized with the contents of the graph named by the resource.
     */
    private static LabelsStore labelsFile(Resource labels) {
        DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
        String labelsURL = GraphUtils.getAsStringValue(labels, VocabAuthzDataset.pLabels);
        if ( labelsURL == null )
            return null;

        try {
            RDFDataMgr.read(dsg, labelsURL);
            return Labels.createLabelsStoreMem(dsg.getDefaultGraph());
        } catch (RiotException ex) {
            FmtLog.error(Secured.BUILD_LOG, "Syntax error in "+labelsURL, ex);
            throw new AssemblerException(labels, "Failed to parse the labels descriptions in"+labelsURL, ex);
        }
    }

    private static boolean subjectInThisAssembler(RDFNode obj) {
        if ( ! obj.isResource() )
            return false;
        return obj.getModel().contains(obj.asResource(), null, (RDFNode)null);
    }

    // RocksDB Store

    /** See whether the resource looks like a RocksDB setup */
    static boolean isRocksDBLabelsStore(Resource resource) {
        return resource.hasProperty(VocabAuthzDataset.pLabelsStorePath);
    }

    /** Create
     *
     * <pre>
     *      authz:labelsStore [  authz:labelsStorePath <file:directory> ] ;
     * </pre>
     *
     * If there are no other settings for the Rocks database, the shorthand form:
     * <pre>
     *      authz:labelsStorePath <file:directory> ;
     * </pre>

     */
    private static LabelsStore createLabelStoreRocksDB(Resource rootLabelStore) {
        // Argument is the object of authz:labelsStore - the blank node in the example above.
        // It can be a resource (URI or blank node) within the assembler configuration file.
        String labelsLocationStr = GraphUtils.getAsStringValue(rootLabelStore, VocabAuthzDataset.pLabelsStorePath);
        // Remove "file:"
        if ( labelsLocationStr.startsWith("file:") )
            labelsLocationStr = IRILib.IRIToFilename(labelsLocationStr);
        Path dbLocation = Path.of(labelsLocationStr);
        File dbDirectory = dbLocation.toFile();

        if ( ! Files.exists(dbLocation) ) {
            // Create directory.
            FmtLog.info(Secured.BUILD_LOG, "Create label store directory %s", dbDirectory.getAbsolutePath());
            dbDirectory.mkdir();
            //throw new AssemblerException(root, "Directory '"+labelsLocationStr+"' does not exist");
        } else if ( ! Files.isDirectory(dbLocation) ) {
            throw new AssemblerException(rootLabelStore, "File location '"+labelsLocationStr+"' is not a directory");
        } else if ( ! Files.isWritable(dbLocation) ) {
            throw new AssemblerException(rootLabelStore, "Directory  '"+labelsLocationStr+"' is not writable");
        }

        // Later:
        // Node Table
        //   Could be shared with RDF DB?
        // Labels.createLabelsStoreRocksDBByNodeId(dbRoot, storeNodeTable, labelMode)

        // Use the string-based variant for now.
        try {
            return generateStore(dbDirectory, rootLabelStore);
        } catch (RocksDBException ex) {
            throw new AssemblerException(rootLabelStore, "Failed to create the RocksDB database", ex);
        }
    }

    /**
     * Create a RocksDB-based label store which stores representations of nodes.
     *
     * @param dbDirectory the root directory of the RocksDB database.
     * @param resource RDF Node representing the given apps configuration
     * @return a labels store which stores its labels in a RocksDB database at {@code dbRoot}
     * @throws RocksDBException if something goes wrong during database creation
     */
    static LabelsStore generateStore(File dbDirectory, Resource resource) throws RocksDBException {
        LabelMode labelMode = getLabelMode(resource);
        StoreFmt storageFmt = getStorageFormat(resource);
        return Labels.createLabelsStoreRocksDB(dbDirectory, labelMode, resource, storageFmt);
    }

    /**
     * Check configuration to see what Storage Format to use
     * @param resource RDF Node representing the given apps configuration
     * @return given format or By String as default/
     */
    static StoreFmt getStorageFormat(Resource resource) {
        if (resource.hasProperty(VocabAuthzDataset.pLabelsStoreByHash))
            return new StoreFmtByHash(getHasher(resource));
        else if (resource.hasProperty(VocabAuthzDataset.pLabelsStoreByString))
            return new StoreFmtByString();
        else
            return new StoreFmtByString();
    }

    /**
     * Check configuration to see whether to merge or overwrite
     * @param resource RDF Node representing the given apps configuration
     * @return given label mode or overwrite by default.
     */
    static LabelMode getLabelMode(Resource resource) {
        if (resource.hasProperty(VocabAuthzDataset.pLabelsStoreUpdateModeOverwrite))
            return LabelMode.OVERWRITE;
        else if (resource.hasProperty(VocabAuthzDataset.pLabelsStoreUpdateModeMerge))
            return LabelMode.MERGE;
        else
            return LabelMode.OVERWRITE;
    }

    /**
     * Check configuration to see which hash function to use.
     * By default, we will use XXX which was the fastest in testing.
     * @param resource RDF Node representing the given apps configuration
     * @return given hash function to use
     */
    static Hasher getHasher(Resource resource) {
        return HasherUtil.obtainHasherFromConfig(GraphUtils.getAsStringValue(resource, VocabAuthzDataset.pLabelsStoreByHashFunction));
    }
}
