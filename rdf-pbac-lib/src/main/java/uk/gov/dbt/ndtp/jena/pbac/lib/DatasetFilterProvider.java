package uk.gov.dbt.ndtp.jena.pbac.lib;

import org.apache.jena.sparql.core.DatasetGraph;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStore;

/**
 * Extension point for deciding how an PBAC-protected request dataset is constructed.
 * <p>
 * Backported from upstream rdf-abac 3.1.2 ({@code io.telicent.jena.abac.DatasetFilterProvider}),
 * confirmed against the real upstream source for SAG-01. Adapted per fork convention:
 * upstream's {@code Label} type is replaced with {@code String}, matching how
 * {@link DatasetGraphPBAC#getDefaultLabel()} already represents the default label in
 * this fork.
 */
public interface DatasetFilterProvider {

    /**
     * Build the filtered dataset for a request against an PBAC-protected dataset.
     *
     * @param dsgAuthz The PBAC dataset wrapper containing the data, labels store, and default label.
     * @param cxt      The PBAC evaluation context (user attributes, hierarchy lookup, tracking).
     * @return A DatasetGraph representing what the request is allowed to see.
     */
    DatasetGraph filterDataset(DatasetGraphPBAC dsgAuthz, CxtPBAC cxt);

    /**
     * Build the filtered dataset for a request against an PBAC-protected dataset.
     *
     * @param dsgBase      The underlying dataset.
     * @param labels       The labels store, or {@code null} to disable label filtering.
     * @param defaultLabel The default label to be applied when none is provided.
     * @param cxt          The PBAC evaluation context.
     * @return A DatasetGraph representing what the request is allowed to see.
     */
    DatasetGraph filterDataset(DatasetGraph dsgBase, LabelsStore labels, String defaultLabel, CxtPBAC cxt);
}
