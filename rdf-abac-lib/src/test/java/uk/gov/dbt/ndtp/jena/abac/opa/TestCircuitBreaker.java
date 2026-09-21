package uk.gov.dbt.ndtp.jena.abac.opa;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import uk.gov.dbt.ndtp.jena.abac.opa.resilience.CircuitBreaker;

public class TestCircuitBreaker {

    @Test
    void startsClosedAndAllowsRequests() {
        CircuitBreaker cb = new CircuitBreaker(3, Duration.ofSeconds(30));
        assertTrue(cb.allowRequest());
        assertEquals(CircuitBreaker.State.CLOSED, cb.state());
    }

    @Test
    void opensAfterThresholdConsecutiveFailures() {
        CircuitBreaker cb = new CircuitBreaker(3, Duration.ofSeconds(30));
        cb.recordFailure();
        cb.recordFailure();
        assertEquals(CircuitBreaker.State.CLOSED, cb.state());
        cb.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, cb.state());
        assertFalse(cb.allowRequest());
    }

    @Test
    void successResetsFailureCount() {
        CircuitBreaker cb = new CircuitBreaker(3, Duration.ofSeconds(30));
        cb.recordFailure();
        cb.recordFailure();
        cb.recordSuccess();
        cb.recordFailure();
        cb.recordFailure();
        assertEquals(CircuitBreaker.State.CLOSED, cb.state());
    }

    @Test
    void halfOpenReturnsToOpenOnFailure() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(1, Duration.ofMillis(10));
        cb.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, cb.state());
        Thread.sleep(20);
        assertTrue(cb.allowRequest());
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.state());
        cb.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, cb.state());
    }

    @Test
    void halfOpenClosesOnSuccess() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(1, Duration.ofMillis(10));
        cb.recordFailure();
        Thread.sleep(20);
        assertTrue(cb.allowRequest());
        cb.recordSuccess();
        assertEquals(CircuitBreaker.State.CLOSED, cb.state());
    }
}