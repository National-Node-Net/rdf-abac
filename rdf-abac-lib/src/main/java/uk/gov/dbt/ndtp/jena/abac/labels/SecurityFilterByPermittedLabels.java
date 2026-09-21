package uk.gov.dbt.ndtp.jena.abac.labels;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import uk.gov.dbt.ndtp.jena.abac.SysABAC;
import uk.gov.dbt.ndtp.jena.abac.lib.QuadFilter;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;

/**
 * Storage-layer filter admitting a triple if every label it carries is in a
 * precomputed permitted set - the "fast membership test" ADR-PF-010 describes as
 * replacing per-triple {@code AE.parseExpr(label).eval(cxt)} evaluation
 * ({@link SecurityFilterByLabel}) once a policy decision has already produced a
 * permitted set for the whole request (SAG-04).
 * <p>
 * No policy evaluation happens here - that has already been done once, before the
 * query ran. This class does only set membership, on purpose: it's the "cheap part"
 * ADR-PF-010 exists to isolate from query-time evaluation cost.
 * <p>
 * Preserves the same multiple-label AND semantics as {@link SecurityFilterByLabel}:
 * if a triple carries several labels, every one must be in the permitted set.
 * <p>
 * Not yet wired into {@code ABAC.filterDataset(...)} - SAG-01
 * ({@code DatasetFilterProvider}) exists and is tested, but the connecting piece
 * ({@code OpaDatasetFilterProvider}, registering this filter through it) is
 * deliberately deferred until SAG-04 merges to {@code develop}. This class itself
 * is ready for that integration.
 */
public class SecurityFilterByPermittedLabels implements QuadFilter {

    private final LabelsGetter labels;
    private final List<String> defaultLookup;
    private final Set<String> permittedLabels;

    public SecurityFilterByPermittedLabels(LabelsGetter labels, String defaultLabel, Set<String> permittedLabels) {
        this.labels = Objects.requireNonNull(labels, "labels");
        this.defaultLookup = (defaultLabel == null)
                ? List.of(SysABAC.SYSTEM_DEFAULT_TRIPLE_ATTRIBUTES)
                : List.of(defaultLabel);
        this.permittedLabels = Objects.requireNonNull(permittedLabels, "permittedLabels");
    }

    @Override
    public boolean test(Quad quad) {
        Triple triple = quad.asTriple();

        List<String> dataLabels = labels.apply(triple);
        if (dataLabels == null) {
            // No labels configured for this dataset at all.
            return SysABAC.DEFAULT_CHOICE_NO_LABELS;
        }

        if (dataLabels.isEmpty()) {
            dataLabels = defaultLookup;
        }

        // AND semantics: every label on the triple must be in the permitted set.
        for (String dataLabel : dataLabels) {
            if (!permittedLabels.contains(dataLabel))
                return false;
        }
        return true;
    }
}
