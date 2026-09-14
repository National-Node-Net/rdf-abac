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


package uk.gov.dbt.ndtp.jena.abac.lib;

import java.util.Set;

import uk.gov.dbt.ndtp.jena.abac.ABAC;
import uk.gov.dbt.ndtp.jena.abac.AE;
import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeExpr;
import uk.gov.dbt.ndtp.jena.abac.labels.Labels;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStoreRocksDB;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.TxnType;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphWrapper;
import org.apache.jena.sparql.core.Transactional;

public class DatasetGraphABAC extends DatasetGraphWrapper {
    // Attribute expression used to determine whether access is allowed.
    // Failing this test, the request is returns with 403 (Forbidden)
    // This may be null, meaning "no test applied".
    private final String accessAttributesStr;
    private final AttributeExpr accessAttributesExpr;
    private final LabelsStore labelsStore;
    // Default for when the labels store has no labels for a triple.
    // Can be null for system-wide policy (which is deny).
    private final String defaultLabel;
    private final AttributesStore attributesStore;

    /** API: use {@link ABAC#authzDataset} */
    public DatasetGraphABAC(DatasetGraph base, String accessAttributes,
                            LabelsStore labelsStore, String datasetDefaultLabel,
                            AttributesStore attributesStore) {
        super(base);
        this.accessAttributesStr = accessAttributes;
        this.accessAttributesExpr = AE.parseExpr(accessAttributesStr);
        this.labelsStore = labelsStore;
        this.defaultLabel = datasetDefaultLabel;
        this.attributesStore = attributesStore;
    }

    /** Get {@link DatasetGraph} being protected */
    public DatasetGraph getData() {
        // Note: DatasetGraphWrapper
        // getBase - recursively unwraps DSG wrapper
        // getWrapped - undoes one level of wrapping and it is the same as protected get().
        // Give the operation a meaningful name for DatasetGraphABAC
        return get();
    }

    public AttributeExpr getAccessAttributes() {
        return accessAttributesExpr;
    }

    public String getDefaultLabel() {
        return defaultLabel;
    }

    public LabelsStore labelsStore() {
        return labelsStore;
    }

    /**
     * The store's labels plus the dataset default. Prefer this over
     * {@link LabelsStore#distinctLabels()}, which omits the default.
     *
     * @throws UnsupportedOperationException if the labels store cannot enumerate its labels
     */
    public Set<String> labelVocabulary() {
        return Labels.vocabulary(labelsStore, defaultLabel);
    }

    @Override
    public void close() {
        super.close();
        if ( labelsStore instanceof LabelsStoreRocksDB x ) {
            try {
                x.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /** Return the attribute store for this dataset. */
    public AttributesStore attributesStore() {
        return attributesStore ;
    }

    /** Return the function for getting the user's attributes for this dataset. */
    public AttributesForUser attributesForUser() {
        return attributesStore::attributes ;
    }

    // Optional per-dataset override of the global DatasetFilterProvider (SAG-01).
    private volatile DatasetFilterProvider filterProvider = null;

    /** The per-dataset filter provider override, or {@code null} if none is set. */
    public DatasetFilterProvider getFilterProvider() {
        return filterProvider;
    }

    /** Register a filter provider for this specific dataset, overriding the global one. */
    public void setFilterProvider(DatasetFilterProvider provider) {
        this.filterProvider = provider;
    }

    // Propagate transactions to the labels store.

    private Transactional getOther() { return labelsStore.getTransactional(); }

    @Override
    public void begin() {
        getOther().begin();
        super.begin();
    }

    @Override
    public ReadWrite transactionMode() {
        getOther().transactionMode();
        return super.transactionMode();
    }

    @Override
    public TxnType transactionType() {
        getOther().transactionType();
        return super.transactionType();
    }

    @Override
    public void begin(TxnType type) {
        // Do begin, then call the other so that "other" is inside the transaction.
        super.begin(type);
        getOther().begin(type);
    }

    @Override
    public void begin(ReadWrite readWrite) {
        super.begin(readWrite);
        getOther().begin(readWrite);
    }

    @Override
    public boolean promote() {
        boolean b = super.promote();
        if ( b )
            getOther().promote();
        return b;
    }

    @Override
    public boolean promote(Promote type) {
        boolean b = super.promote(type);
        if ( b )
            getOther().promote(type);
        return b;
    }

    @Override
    public void commit() {
        getOther().commit();
        super.commit();
    }

    @Override
    public void abort() {
        getOther().abort();
        super.abort();
    }

    @Override
    public void end() {
        getOther().end();
        super.end();
    }

    @Override
    public boolean isInTransaction() {
        getOther().isInTransaction();
        return super.isInTransaction();
    }

    @Override
    public boolean supportsTransactions() {
        // DatasetGraph operation.
        //getOther().supportsTransactions();
        return super.supportsTransactions();
    }

    @Override
    public boolean supportsTransactionAbort() {
        // DatasetGraph operation.
        //getOther().supportsTransactionAbort();
        return super.supportsTransactionAbort();
    }
}
