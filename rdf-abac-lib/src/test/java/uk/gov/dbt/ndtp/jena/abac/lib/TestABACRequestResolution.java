package uk.gov.dbt.ndtp.jena.abac.lib;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import uk.gov.dbt.ndtp.jena.abac.ABAC;
import uk.gov.dbt.ndtp.jena.abac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.abac.Hierarchy;
import uk.gov.dbt.ndtp.jena.abac.labels.Labels;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStore;

public class TestABACRequestResolution {

    @AfterEach
    void resetRegistrations() {
        ABACRequest.reset();
    }

    private static CxtABAC ctx(DatasetGraph dsg) {
        return CxtABAC.context(AttributeValueSet.of(java.util.List.of()), Hierarchy.noHierarchy, dsg);
    }

    private static DatasetGraphABAC authzDataset() {
        LabelsStore store = Labels.createLabelsStoreMem();
        DatasetGraph base = DatasetGraphFactory.createTxnMem();
        return ABAC.authzDataset(base, store, "dflt", null);
    }

    @Test
    void noProviderRegistered_resolvesToDefault() {
        DatasetGraphABAC dsgz = authzDataset();
        assertSame(DefaultDatasetFilterProvider.INSTANCE, ABACRequest.resolveProvider(dsgz));
    }

    @Test
    void globalProvider_usedWhenNoPerDatasetProvider() {
        DatasetGraphABAC dsgz = authzDataset();
        DatasetFilterProvider custom = new StubProvider();

        ABACRequest.setFilterProvider(custom);

        assertSame(custom, ABACRequest.resolveProvider(dsgz));
    }

    @Test
    void perDatasetProvider_takesPriorityOverGlobal() {
        DatasetGraphABAC dsgz = authzDataset();
        DatasetFilterProvider global = new StubProvider();
        DatasetFilterProvider perDataset = new StubProvider();

        ABACRequest.setFilterProvider(global);
        ABACRequest.setDatasetFilterProvider(dsgz, perDataset);

        assertSame(perDataset, ABACRequest.resolveProvider(dsgz));
    }

    @Test
    void clearingPerDatasetProvider_fallsBackToGlobal() {
        DatasetGraphABAC dsgz = authzDataset();
        DatasetFilterProvider global = new StubProvider();
        DatasetFilterProvider perDataset = new StubProvider();

        ABACRequest.setFilterProvider(global);
        ABACRequest.setDatasetFilterProvider(dsgz, perDataset);
        ABACRequest.setDatasetFilterProvider(dsgz, null);

        assertSame(global, ABACRequest.resolveProvider(dsgz));
    }

    @Test
    void filterDataset_noProviderRegistered_behaviourUnchanged() {
        DatasetGraphABAC dsgz = authzDataset();
        assertDoesNotThrow(() -> ABAC.filterDataset(dsgz, ctx(dsgz.getData())));
    }

    @Test
    void filterDataset_customProviderRegistered_isUsed() {
        DatasetGraphABAC dsgz = authzDataset();
        java.util.concurrent.atomic.AtomicBoolean called = new java.util.concurrent.atomic.AtomicBoolean(false);

        ABACRequest.setDatasetFilterProvider(dsgz, new DatasetFilterProvider() {
            @Override
            public DatasetGraph filterDataset(DatasetGraphABAC d, CxtABAC c) {
                called.set(true);
                return d.getData();
            }
            @Override
            public DatasetGraph filterDataset(DatasetGraph b, LabelsStore l, String def, CxtABAC c) {
                called.set(true);
                return b;
            }
        });

        ABAC.filterDataset(dsgz, ctx(dsgz.getData()));

        assertTrue(called.get());
    }

    @Test
    void setFilterProvider_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ABACRequest.setFilterProvider(null));
    }

    @Test
    void filterDataset_rawParts_usesGlobalProvider() {
        LabelsStore store = Labels.createLabelsStoreMem();
        DatasetGraph base = DatasetGraphFactory.createTxnMem();
        CxtABAC cxt = ctx(base);
        DatasetFilterProvider custom = new StubProvider();

        ABACRequest.setFilterProvider(custom);

        DatasetGraph result = ABAC.filterDataset(base, store, "dflt", cxt);

        assertSame(base, result);
    }

    private static class StubProvider implements DatasetFilterProvider {
        @Override public DatasetGraph filterDataset(DatasetGraphABAC d, CxtABAC c) { return d.getData(); }
        @Override public DatasetGraph filterDataset(DatasetGraph b, LabelsStore l, String def, CxtABAC c) { return b; }
    }
}
