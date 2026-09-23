package uk.gov.dbt.ndtp.jena.pbac.lib;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import uk.gov.dbt.ndtp.jena.pbac.PBAC;
import uk.gov.dbt.ndtp.jena.pbac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.pbac.Hierarchy;
import uk.gov.dbt.ndtp.jena.pbac.labels.Labels;
import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStore;

public class TestPBACRequestResolution {

    @AfterEach
    void resetRegistrations() {
        PBACRequest.reset();
    }

    private static CxtPBAC ctx(DatasetGraph dsg) {
        return CxtPBAC.context(AttributeValueSet.of(java.util.List.of()), Hierarchy.noHierarchy, dsg);
    }

    private static DatasetGraphPBAC authzDataset() {
        LabelsStore store = Labels.createLabelsStoreMem();
        DatasetGraph base = DatasetGraphFactory.createTxnMem();
        return PBAC.authzDataset(base, store, "dflt", null);
    }

    @Test
    void noProviderRegistered_resolvesToDefault() {
        DatasetGraphPBAC dsgz = authzDataset();
        assertSame(DefaultDatasetFilterProvider.INSTANCE, PBACRequest.resolveProvider(dsgz));
    }

    @Test
    void globalProvider_usedWhenNoPerDatasetProvider() {
        DatasetGraphPBAC dsgz = authzDataset();
        DatasetFilterProvider custom = new StubProvider();

        PBACRequest.setFilterProvider(custom);

        assertSame(custom, PBACRequest.resolveProvider(dsgz));
    }

    @Test
    void perDatasetProvider_takesPriorityOverGlobal() {
        DatasetGraphPBAC dsgz = authzDataset();
        DatasetFilterProvider global = new StubProvider();
        DatasetFilterProvider perDataset = new StubProvider();

        PBACRequest.setFilterProvider(global);
        PBACRequest.setDatasetFilterProvider(dsgz, perDataset);

        assertSame(perDataset, PBACRequest.resolveProvider(dsgz));
    }

    @Test
    void clearingPerDatasetProvider_fallsBackToGlobal() {
        DatasetGraphPBAC dsgz = authzDataset();
        DatasetFilterProvider global = new StubProvider();
        DatasetFilterProvider perDataset = new StubProvider();

        PBACRequest.setFilterProvider(global);
        PBACRequest.setDatasetFilterProvider(dsgz, perDataset);
        PBACRequest.setDatasetFilterProvider(dsgz, null);

        assertSame(global, PBACRequest.resolveProvider(dsgz));
    }

    @Test
    void filterDataset_noProviderRegistered_behaviourUnchanged() {
        DatasetGraphPBAC dsgz = authzDataset();
        assertDoesNotThrow(() -> PBAC.filterDataset(dsgz, ctx(dsgz.getData())));
    }

    @Test
    void filterDataset_customProviderRegistered_isUsed() {
        DatasetGraphPBAC dsgz = authzDataset();
        java.util.concurrent.atomic.AtomicBoolean called = new java.util.concurrent.atomic.AtomicBoolean(false);

        PBACRequest.setDatasetFilterProvider(dsgz, new DatasetFilterProvider() {
            @Override
            public DatasetGraph filterDataset(DatasetGraphPBAC d, CxtPBAC c) {
                called.set(true);
                return d.getData();
            }
            @Override
            public DatasetGraph filterDataset(DatasetGraph b, LabelsStore l, String def, CxtPBAC c) {
                called.set(true);
                return b;
            }
        });

        PBAC.filterDataset(dsgz, ctx(dsgz.getData()));

        assertTrue(called.get());
    }

    @Test
    void setFilterProvider_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> PBACRequest.setFilterProvider(null));
    }

    @Test
    void filterDataset_rawParts_usesGlobalProvider() {
        LabelsStore store = Labels.createLabelsStoreMem();
        DatasetGraph base = DatasetGraphFactory.createTxnMem();
        CxtPBAC cxt = ctx(base);
        DatasetFilterProvider custom = new StubProvider();

        PBACRequest.setFilterProvider(custom);

        DatasetGraph result = PBAC.filterDataset(base, store, "dflt", cxt);

        assertSame(base, result);
    }

    private static class StubProvider implements DatasetFilterProvider {
        @Override public DatasetGraph filterDataset(DatasetGraphPBAC d, CxtPBAC c) { return d.getData(); }
        @Override public DatasetGraph filterDataset(DatasetGraph b, LabelsStore l, String def, CxtPBAC c) { return b; }
    }
}
