package uk.gov.dbt.ndtp.jena.abac.opa.resilience;

import uk.gov.dbt.ndtp.jena.abac.opa.*;

import java.util.Objects;
import java.util.Set;

/**
 * Wraps a {@link DecisionServiceProvider}, opening the circuit after repeated
 * unavailability so SAG stops sending doomed requests to a known-down OPA instance
 * instead of waiting out a full connect/read timeout on every single one.
 * <p>
 * Sits around {@link OpaDecisionServiceProvider}, not inside it - the wire-level
 * provider stays free of circuit-breaking policy (which failure threshold, how long
 * to stay open), since that's an operational tuning concern, closer to SAG-06
 * (configuration) than to the decision logic itself.
 */
public class CircuitBreakingDecisionServiceProvider implements DecisionServiceProvider {

    private final DecisionServiceProvider delegate;
    private final CircuitBreaker circuitBreaker;

    public CircuitBreakingDecisionServiceProvider(DecisionServiceProvider delegate, CircuitBreaker circuitBreaker) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker");
    }

    @Override
    public DecisionResult decide(DecisionContext context, Set<String> vocabulary) {
        if (!circuitBreaker.allowRequest())
            throw new DecisionServiceUnavailableException("Circuit open: OPA presumed unavailable");

        try {
            DecisionResult result = delegate.decide(context, vocabulary);
            circuitBreaker.recordSuccess();
            return result;
        } catch (DecisionServiceUnavailableException e) {
            circuitBreaker.recordFailure();
            throw e;
        }
        // An empty DecisionResult reaching here is a genuine "OPA said permit nothing"
        // outcome (confirmed with the team see DecisionServiceUnavailableException javadoc),
        // never a transport failure, so it correctly does not count against the circuit.
    }
}
