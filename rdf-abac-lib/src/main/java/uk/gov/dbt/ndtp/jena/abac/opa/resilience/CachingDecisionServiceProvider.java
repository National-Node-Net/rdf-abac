package uk.gov.dbt.ndtp.jena.abac.opa.resilience;

import uk.gov.dbt.ndtp.jena.abac.opa.DecisionContext;
import uk.gov.dbt.ndtp.jena.abac.opa.DecisionResult;
import uk.gov.dbt.ndtp.jena.abac.opa.DecisionServiceProvider;

import java.util.Objects;
import java.util.Set;

/**
 * Ensures at most one real decision is made per request, even if multiple call sites
 * (SAG-05's admission check, SAG-01's filter provider) each ask for one against the
 * same {@link DecisionContext#cxt()}. Relies on {@code ABAC_Request.decideDataset()}
 * building one {@code CxtABAC} and threading it through to {@code filterDataset(...)}
 * unchanged, as it already does today - whoever builds SAG-05 must preserve that, or
 * this cache never hits and every request pays for two OPA calls instead of one.
 */
public class CachingDecisionServiceProvider implements DecisionServiceProvider {

    private final DecisionServiceProvider delegate;

    public CachingDecisionServiceProvider(DecisionServiceProvider delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public DecisionResult decide(DecisionContext context, Set<String> vocabulary) {
        Object cached = context.cxt().attachment();
        if (cached instanceof DecisionResult result)
            return result;

        DecisionResult result = delegate.decide(context, vocabulary);
        context.cxt().attachment(result);
        return result;
    }
}
