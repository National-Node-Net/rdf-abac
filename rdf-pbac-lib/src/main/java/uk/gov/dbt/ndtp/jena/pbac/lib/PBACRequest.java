package uk.gov.dbt.ndtp.jena.pbac.lib;

/**
 * Static registration facade for {@link DatasetFilterProvider}s, per SAG-01 AC3
 * (static registration, not ServiceLoader). Mirrors upstream's model: a single global
 * provider (held here), and a per-dataset override stored directly on
 * {@link DatasetGraphPBAC} — not a separate registry — confirmed against the real
 * upstream {@code DatasetGraphABAC} source.
 */
public final class PBACRequest {

    private PBACRequest() {}

    private static volatile DatasetFilterProvider globalProvider = DefaultDatasetFilterProvider.INSTANCE;

    /** Register a provider for one specific dataset. Takes priority over the global provider. */
    public static void setDatasetFilterProvider(DatasetGraphPBAC dataset, DatasetFilterProvider provider) {
        dataset.setFilterProvider(provider);
    }

    /** Register a provider for all datasets that don't have their own. */
    public static void setFilterProvider(DatasetFilterProvider provider) {
        java.util.Objects.requireNonNull(provider, "DatasetFilterProvider cannot be null");
        globalProvider = provider;
    }

    /** The current global provider. */
    public static DatasetFilterProvider getFilterProvider() {
        return globalProvider;
    }

    /** SAG-01 AC2: per-dataset, then global. Never returns null. */
    public static DatasetFilterProvider resolveProvider(DatasetGraphPBAC dsgAuthz) {
        DatasetFilterProvider perDataset = dsgAuthz.getFilterProvider();
        return (perDataset != null) ? perDataset : globalProvider;
    }

    /** Test/reset helper — restores default-only behaviour. */
    public static void reset() {
        globalProvider = DefaultDatasetFilterProvider.INSTANCE;
    }
}
