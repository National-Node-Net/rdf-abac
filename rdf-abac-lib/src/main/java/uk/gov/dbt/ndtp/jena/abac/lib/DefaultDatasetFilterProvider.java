package uk.gov.dbt.ndtp.jena.abac.lib;

import java.util.Set;

import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFilteredView;
import uk.gov.dbt.ndtp.jena.abac.labels.Labels;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsGetter;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStore;

/**
 * The provider used when nothing is registered — reproduces the pre-SAG-01 body of
 * {@code ABAC.filterDataset(...)} unchanged, so any dataset with no provider registered
 * sees no behavioural difference (SAG-01 AC5).
 */
public final class DefaultDatasetFilterProvider implements DatasetFilterProvider {

    public static final DefaultDatasetFilterProvider INSTANCE = new DefaultDatasetFilterProvider();

    private DefaultDatasetFilterProvider() {}

    @Override
    public DatasetGraph filterDataset(DatasetGraphABAC dsgAuthz, CxtABAC cxt) {
        return filterDataset(dsgAuthz.getData(), dsgAuthz.labelsStore(), dsgAuthz.getDefaultLabel(), cxt);
    }

    @Override
    public DatasetGraph filterDataset(DatasetGraph dsgBase, LabelsStore labels, String defaultLabel, CxtABAC cxt) {
        QuadFilter filter = null;
        if (labels != null) {
            LabelsGetter getter = labels::labelsForTriples;
            filter = Labels.securityFilterByLabel(dsgBase, getter, defaultLabel, cxt);
        }
        return new DatasetGraphFilteredView(dsgBase, filter, Set.of());
    }
}
