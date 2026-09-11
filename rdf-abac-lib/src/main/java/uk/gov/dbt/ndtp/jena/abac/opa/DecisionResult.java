package uk.gov.dbt.ndtp.jena.abac.opa;

import java.util.Map;
import java.util.Set;

/**
 * The outcome of a policy decision: the subset of the submitted vocabulary the requester
 * is permitted to see, plus opaque audit metadata (shape TBC, ADR-PF-006).
 * <p>
 * An empty {@code permittedLabels} is a valid, meaningful result - "nothing in this
 * dataset is visible to this requester"- produced only from a genuine, well-formed OPA
 * response. True unreachability or an untrustworthy response is signalled separately by
 * {@link DecisionServiceUnavailableException}; see that class's javadoc for the confirmed
 * split.
 */
public final class DecisionResult {

    private final Set<String> permittedLabels;
    private final Map<String, Object> auditMetadata;

    public DecisionResult(Set<String> permittedLabels, Map<String, Object> auditMetadata) {
        this.permittedLabels = Set.copyOf(permittedLabels);
        this.auditMetadata = auditMetadata == null ? Map.of() : Map.copyOf(auditMetadata);
    }

    public Set<String> permittedLabels() { return permittedLabels; }
    public Map<String, Object> auditMetadata() { return auditMetadata; }

    public boolean isEmpty() { return permittedLabels.isEmpty(); }

    public static DecisionResult empty() {
        return new DecisionResult(Set.of(), Map.of());
    }
}
