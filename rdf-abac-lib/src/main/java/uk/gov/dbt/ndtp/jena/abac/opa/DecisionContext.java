package uk.gov.dbt.ndtp.jena.abac.opa;

import java.util.Objects;
import uk.gov.dbt.ndtp.jena.abac.lib.CxtABAC;

/**
 * Context for a single policy decision request to a {@link DecisionServiceProvider}.
 * <p>
 * Wraps the existing {@link CxtABAC} (subject attributes, request id, per-request caches)
 * and adds the fields a PBAC decision needs that CxtABAC does not yet carry: who is asking
 * (subjectId), the action being performed, the organisation the requester belongs to, and
 * the dataset being queried.
 * <p>
 * SAG (the PEP) sends all resolved subject attributes to OPA in the
 * request - OPA does not call back to the PIP itself. {@code subjectId} identifies who
 * those attributes belong to, for OPA's audit/reasoning, separate from the attribute
 * values themselves (which live on the wrapped {@link CxtABAC}).
 * <p>
 * {@code organisationId} is a placeholder until SAG-03 (Decision Context) lands and
 * organisation identity is extracted from verified JWT claims (per SAG-05 AC2). Until
 * then callers must supply it themselves; it is not derived here.
 */
public class DecisionContext {

    private final CxtABAC cxt;
    private final String subjectId;
    private final String action;
    private final String organisationId;
    private final String datasetName;

    public DecisionContext(CxtABAC cxt, String subjectId, String action, String organisationId, String datasetName) {
        this.cxt = Objects.requireNonNull(cxt, "cxt");
        this.subjectId = Objects.requireNonNull(subjectId, "subjectId");
        this.action = Objects.requireNonNull(action, "action");
        this.organisationId = organisationId; // nullable until SAG-03
        this.datasetName = Objects.requireNonNull(datasetName, "datasetName");
    }

    public CxtABAC cxt() { return cxt; }
    public String subjectId() { return subjectId; }
    public String action() { return action; }
    public String organisationId() { return organisationId; }
    public String datasetName() { return datasetName; }

    /** Convenience: the request id carried by the underlying CxtABAC. */
    public Object requestId() { return cxt.requestId(); }
}
