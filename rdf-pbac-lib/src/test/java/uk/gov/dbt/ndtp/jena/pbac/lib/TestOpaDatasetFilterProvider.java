package uk.gov.dbt.ndtp.jena.pbac.lib;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.junit.jupiter.api.Test;
import uk.gov.dbt.ndtp.jena.pbac.PBAC;
import uk.gov.dbt.ndtp.jena.pbac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.pbac.Hierarchy;
import uk.gov.dbt.ndtp.jena.pbac.labels.Labels;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.pbac.opa.DecisionResult;
import uk.gov.dbt.ndtp.jena.pbac.opa.DecisionServiceProvider;
import uk.gov.dbt.ndtp.jena.pbac.opa.PolicyDeniedException;

public class TestOpaDatasetFilterProvider {

    private static CxtPBAC ctx(DatasetGraph dsg) {
        CxtPBAC cxt = CxtPBAC.context(AttributeValueSet.of(java.util.List.of()), Hierarchy.noHierarchy, dsg);
        cxt.subjectId("test-user");
        cxt.action("read");
        cxt.datasetName("test-dataset");
        return cxt;
    }

    private static DatasetGraphPBAC authzDataset() {
        LabelsStore store = Labels.createLabelsStoreMem();
        DatasetGraph base = DatasetGraphFactory.createTxnMem();
        return PBAC.authzDataset(base, store, "dflt", null);
    }

    @Test
    void filterDataset_callsDecisionServiceAndBuildsFilteredView() {
        DecisionServiceProvider stub = (context, vocabulary) -> new DecisionResult(Set.of("public"), java.util.Map.of());
        OpaDatasetFilterProvider provider = new OpaDatasetFilterProvider(stub);
        DatasetGraphPBAC dsgz = authzDataset();

        DatasetGraph result = provider.filterDataset(dsgz, ctx(dsgz.getData()));

        assertNotNull(result);
    }

    @Test
    void filterDataset_rawParts_worksWithoutDatasetGraphPBAC() {
        DecisionServiceProvider stub = (context, vocabulary) -> new DecisionResult(Set.of("public"), java.util.Map.of());
        OpaDatasetFilterProvider provider = new OpaDatasetFilterProvider(stub);
        LabelsStore store = Labels.createLabelsStoreMem();
        DatasetGraph base = DatasetGraphFactory.createTxnMem();

        DatasetGraph result = provider.filterDataset(base, store, "dflt", ctx(base));

        assertNotNull(result);
    }

    @Test
    void filterDataset_emptyPermittedLabels_throwsPolicyDenied() {
        DecisionServiceProvider stub = (context, vocabulary) -> DecisionResult.empty();
        OpaDatasetFilterProvider provider = new OpaDatasetFilterProvider(stub);
        DatasetGraphPBAC dsgz = authzDataset();

        assertThrows(PolicyDeniedException.class,
                () -> provider.filterDataset(dsgz, ctx(dsgz.getData())));
    }

    @Test
    void filterDataset_rawParts_emptyPermittedLabels_throwsPolicyDenied() {
        DecisionServiceProvider stub = (context, vocabulary) -> DecisionResult.empty();
        OpaDatasetFilterProvider provider = new OpaDatasetFilterProvider(stub);
        LabelsStore store = Labels.createLabelsStoreMem();
        DatasetGraph base = DatasetGraphFactory.createTxnMem();

        assertThrows(PolicyDeniedException.class,
                () -> provider.filterDataset(base, store, "dflt", ctx(base)));
    }
}
