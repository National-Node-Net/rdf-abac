package uk.gov.dbt.ndtp.jena.abac.opa.resilience;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Simple three-state circuit breaker (CLOSED / OPEN / HALF_OPEN) guarding calls to a
 * downstream decision service. Purely a call-admission gate — it does not know what
 * "success" or "failure" means for the caller; {@link CircuitBreakingDecisionServiceProvider}
 * decides that and reports it via {@link #recordSuccess()} / {@link #recordFailure()}.
 * <p>
 * Not thread-contention-optimised (synchronized methods) - request volume through a
 * single SAG node's OPA client does not warrant anything fancier here.
 */
public class CircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final Duration openDuration;

    private int consecutiveFailures = 0;
    private State state = State.CLOSED;
    private Instant openedAt;

    public CircuitBreaker(int failureThreshold, Duration openDuration) {
        if (failureThreshold <= 0)
            throw new IllegalArgumentException("failureThreshold must be > 0");
        this.failureThreshold = failureThreshold;
        this.openDuration = Objects.requireNonNull(openDuration, "openDuration");
    }

    /**
     * Whether a call should be attempted right now. OPEN blocks calls until
     * {@code openDuration} has elapsed, then allows one trial call (HALF_OPEN).
     */
    public synchronized boolean allowRequest() {
        return switch (state) {
            case CLOSED -> true;
            case HALF_OPEN -> true;
            case OPEN -> {
                if (Instant.now().isAfter(openedAt.plus(openDuration))) {
                    state = State.HALF_OPEN;
                    yield true;
                }
                yield false;
            }
        };
    }

    public synchronized void recordSuccess() {
        consecutiveFailures = 0;
        state = State.CLOSED;
        openedAt = null;
    }

    public synchronized void recordFailure() {
        consecutiveFailures++;
        if (state == State.HALF_OPEN || consecutiveFailures >= failureThreshold) {
            state = State.OPEN;
            openedAt = Instant.now();
        }
    }

    public synchronized State state() { return state; }
}