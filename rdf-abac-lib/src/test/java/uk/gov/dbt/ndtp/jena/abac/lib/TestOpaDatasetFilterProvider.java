package uk.gov.dbt.ndtp.jena.abac.lib;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.junit.jupiter.api.Test;
import uk.gov.dbt.ndtp.jena.abac.ABAC;
import uk.gov.dbt.ndtp.jena.abac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.abac.Hierarchy;
import uk.gov.dbt.ndtp.jena.abac.labels.Labels;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.abac.opa.DecisionResult;
import uk.gov.dbt.ndtp.jena.abac.opa.DecisionServiceProvider;

public class TestOpaDatasetFilterProvider {

    private static CxtABAC ctx(DatasetGraph dsg) {
        return CxtABAC.context(AttributeValueSet.of(java.util.List.of()), Hierarchy.noHierarchy, dsg);
    }

    private static DatasetGraphABAC authzDataset() {
        LabelsStore store = Labels.createLabelsStoreMem();
        DatasetGraph base = DatasetGraphFactory.createTxnMem();
        return ABAC.authzDataset(base, store, "dflt", null);
    }

    @Test
    void filterDataset_callsDecisionServiceAndBuildsFilteredView() {
        DecisionServiceProvider stub = (context, vocabulary) -> new DecisionResult(Set.of("public"), java.util.Map.of());
        OpaDatasetFilterProvider provider = new OpaDatasetFilterProvider(stub);
        DatasetGraphABAC dsgz = authzDataset();

        DatasetGraph result = provider.filterDataset(dsgz, ctx(dsgz.getData()));

        assertNotNull(result);
    }

    @Test
    void filterDataset_rawParts_worksWithoutDatasetGraphABAC() {
        DecisionServiceProvider stub = (context, vocabulary) -> new DecisionResult(Set.of("public"), java.util.Map.of());
        OpaDatasetFilterProvider provider = new OpaDatasetFilterProvider(stub);
        LabelsStore store = Labels.createLabelsStoreMem();
        DatasetGraph base = DatasetGraphFactory.createTxnMem();

        DatasetGraph result = provider.filterDataset(base, store, "dflt", ctx(base));

        assertNotNull(result);
    }
}
