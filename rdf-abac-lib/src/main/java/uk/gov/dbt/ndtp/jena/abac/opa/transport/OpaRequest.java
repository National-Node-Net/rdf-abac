package uk.gov.dbt.ndtp.jena.abac.opa.transport;

import uk.gov.dbt.ndtp.jena.abac.opa.DecisionContext;

import java.util.Set;

/**
 * What SAG sends to OPA. Field shape is provisional - TODO once the real contract lands.
 * <p>
 * Confirmed (two points):
 * 1. SAG (the PEP) resolves and sends ALL of the subject's attributes in this request -
 *    OPA never calls back to the PIP itself ("all prefetched and sent to it via PEP").
 * 2. Attributes are sent RAW, not hierarchy-expanded. "Rego does the hierarchy comparison
 *    itself" - "No decision making happens in the SAG in theory". So a user holding
 *    "clearance=secret" is sent exactly that, never also "clearance=ordinary". Hierarchy
 *    definitions themselves are therefore NOT part of this per-request payload - if OPA
 *    needs them, they're presumably loaded into OPA separately (as policy/bundle data),
 *    not sent with every request. No field for them here as a result.
 */
public final class OpaRequest {

    private final String subjectId;
    private final String action;
    private final String organisationId;
    private final String datasetName;
    private final Set<String> vocabulary;
    private final Set<String> subjectAttributes;

    private OpaRequest(String subjectId, String action, String organisationId, String datasetName,
                       Set<String> vocabulary, Set<String> subjectAttributes) {
        this.subjectId = subjectId;
        this.action = action;
        this.organisationId = organisationId;
        this.datasetName = datasetName;
        this.vocabulary = Set.copyOf(vocabulary);
        this.subjectAttributes = Set.copyOf(subjectAttributes);
    }

    /**
     * {@code subjectAttributes} must be the requester's raw, resolved attribute set -
     * confirmed: SAG sends all attributes as-is, never hierarchy-expanded.
     */
    public static OpaRequest of(DecisionContext context, Set<String> vocabulary, Set<String> subjectAttributes) {
        return new OpaRequest(context.subjectId(), context.action(), context.organisationId(),
                context.datasetName(), vocabulary, subjectAttributes);
    }

    public String subjectId() { return subjectId; }
    public String action() { return action; }
    public String organisationId() { return organisationId; }
    public String datasetName() { return datasetName; }
    public Set<String> vocabulary() { return vocabulary; }
    public Set<String> subjectAttributes() { return subjectAttributes; }
}
