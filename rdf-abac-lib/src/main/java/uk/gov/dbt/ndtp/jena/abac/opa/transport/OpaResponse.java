package uk.gov.dbt.ndtp.jena.abac.opa.transport;

import java.util.Map;
import java.util.Set;

/** What OPA returns, parsed. Shape provisional pending the real contract. */
public final class OpaResponse {
    private final Set<String> permittedLabels;
    private final Map<String, Object> auditMetadata;

    public OpaResponse(Set<String> permittedLabels, Map<String, Object> auditMetadata) {
        this.permittedLabels = permittedLabels;
        this.auditMetadata = auditMetadata;
    }

    public Set<String> permittedLabels() { return permittedLabels; }
    public Map<String, Object> auditMetadata() { return auditMetadata; }
}
