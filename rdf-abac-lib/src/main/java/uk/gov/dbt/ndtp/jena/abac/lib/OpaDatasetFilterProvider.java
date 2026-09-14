package uk.gov.dbt.ndtp.jena.abac.lib;

import java.util.Set;

import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFilteredView;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsGetter;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.abac.labels.Labels;
import uk.gov.dbt.ndtp.jena.abac.labels.SecurityFilterByPermittedLabels;
import uk.gov.dbt.ndtp.jena.abac.opa.DecisionContext;
import uk.gov.dbt.ndtp.jena.abac.opa.DecisionResult;
import uk.gov.dbt.ndtp.jena.abac.opa.DecisionServiceProvider;

/**
 * The SAG-01 / SAG-04 connecting piece: a {@link DatasetFilterProvider} that calls an
 * OPA-backed {@link DecisionServiceProvider} for the permitted label set, then builds a
 * {@link SecurityFilterByPermittedLabels} from it instead of the legacy per-triple
 * expression evaluator.
 * <p>
 * KNOWN GAP: {@code action} and {@code datasetName} are not available at this layer today
 * - {@link DatasetGraphABAC} carries no operation name or dataset name; that information
 * only exists further up, in the Fuseki layer ({@code ABAC_Request}, via
 * {@code HttpAction}). Both are placeholders here until that's threaded through (likely
 * overlaps SAG-03's remit, since {@code organisationId} has the same gap).
 */
public class OpaDatasetFilterProvider implements DatasetFilterProvider {

    private final DecisionServiceProvider decisionService;

    public OpaDatasetFilterProvider(DecisionServiceProvider decisionService) {
        this.decisionService = decisionService;
    }

    @Override
    public DatasetGraph filterDataset(DatasetGraphABAC dsgAuthz, CxtABAC cxt) {
        Set<String> vocabulary = dsgAuthz.labelVocabulary();

        // TODO: subjectId/action/organisationId/datasetName are placeholders - see class javadoc.
        DecisionContext context = new DecisionContext(cxt, subjectIdOf(cxt), "query", null, "unknown");
        DecisionResult result = decisionService.decide(context, vocabulary);

        LabelsGetter getter = dsgAuthz.labelsStore()::labelsForTriples;
        SecurityFilterByPermittedLabels filter =
                new SecurityFilterByPermittedLabels(getter, dsgAuthz.getDefaultLabel(), result.permittedLabels());

        return new DatasetGraphFilteredView(dsgAuthz.getData(), filter, Set.of());
    }

    @Override
    public DatasetGraph filterDataset(DatasetGraph dsgBase, LabelsStore labels, String defaultLabel, CxtABAC cxt) {
        Set<String> vocabulary = (labels != null)
                ? Labels.vocabulary(labels, defaultLabel)
                : Set.of();

        DecisionContext context = new DecisionContext(cxt, subjectIdOf(cxt), "query", null, "unknown");
        DecisionResult result = decisionService.decide(context, vocabulary);

        if (labels == null)
            return new DatasetGraphFilteredView(dsgBase, null, Set.of());

        LabelsGetter getter = labels::labelsForTriples;
        SecurityFilterByPermittedLabels filter =
                new SecurityFilterByPermittedLabels(getter, defaultLabel, result.permittedLabels());

        return new DatasetGraphFilteredView(dsgBase, filter, Set.of());
    }

    // TODO: placeholder - subjectId should come from verified identity, not CxtABAC directly.
    private static String subjectIdOf(CxtABAC cxt) {
        return "unknown-subject";
    }
}
