package uk.gov.dbt.ndtp.jena.pbac.lib;

import java.util.Set;

import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFilteredView;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsGetter;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.pbac.labels.Labels;
import uk.gov.dbt.ndtp.jena.pbac.labels.SecurityFilterByPermittedLabels;
import uk.gov.dbt.ndtp.jena.pbac.opa.DecisionContext;
import uk.gov.dbt.ndtp.jena.pbac.opa.DecisionResult;
import uk.gov.dbt.ndtp.jena.pbac.opa.DecisionServiceProvider;
import uk.gov.dbt.ndtp.jena.pbac.opa.PolicyDeniedException;

/**
 * The SAG-01 / SAG-04 connecting piece: a {@link DatasetFilterProvider} that calls an
 * OPA-backed {@link DecisionServiceProvider} for the permitted label set, then builds a
 * {@link SecurityFilterByPermittedLabels} from it instead of the legacy per-triple
 * expression evaluator.
 * <p>
 * - {@link DatasetGraphPBAC} carries no operation name or dataset name; that information
 * only exists further up, in the Fuseki layer ({@code PBAC_Request}, via
 * {@code HttpAction}). Both are placeholders here until that's threaded through (likely
 * overlaps SAG-03's remit, since {@code organisationId} has the same gap).
 */
public class OpaDatasetFilterProvider implements DatasetFilterProvider {

    private final DecisionServiceProvider decisionService;

    public OpaDatasetFilterProvider(DecisionServiceProvider decisionService) {
        this.decisionService = decisionService;
    }

    @Override
    public DatasetGraph filterDataset(DatasetGraphPBAC dsgAuthz, CxtPBAC cxt) {
        Set<String> vocabulary = dsgAuthz.labelVocabulary();

        DecisionContext context = new DecisionContext(cxt, cxt.subjectId(), cxt.action(), cxt.organisationId(), cxt.datasetName());
        DecisionResult result = decisionService.decide(context, vocabulary);
        if ( result.isEmpty() )
            throw new PolicyDeniedException("No permitted labels for subject = " + cxt.subjectId());

        LabelsGetter getter = dsgAuthz.labelsStore()::labelsForTriples;
        SecurityFilterByPermittedLabels filter =
                new SecurityFilterByPermittedLabels(getter, dsgAuthz.getDefaultLabel(), result.permittedLabels());

        return new DatasetGraphFilteredView(dsgAuthz.getData(), filter, Set.of());
    }

    @Override
    public DatasetGraph filterDataset(DatasetGraph dsgBase, LabelsStore labels, String defaultLabel, CxtPBAC cxt) {
        Set<String> vocabulary = (labels != null)
                ? Labels.vocabulary(labels, defaultLabel)
                : Set.of();

        DecisionContext context = new DecisionContext(cxt, cxt.subjectId(), cxt.action(), cxt.organisationId(), cxt.datasetName());
        DecisionResult result = decisionService.decide(context, vocabulary);
        if ( result.isEmpty() )
            throw new PolicyDeniedException("No permitted labels for subject = " + cxt.subjectId());

        if (labels == null)
            return new DatasetGraphFilteredView(dsgBase, null, Set.of());

        LabelsGetter getter = labels::labelsForTriples;
        SecurityFilterByPermittedLabels filter =
                new SecurityFilterByPermittedLabels(getter, defaultLabel, result.permittedLabels());

        return new DatasetGraphFilteredView(dsgBase, filter, Set.of());
    }

}
