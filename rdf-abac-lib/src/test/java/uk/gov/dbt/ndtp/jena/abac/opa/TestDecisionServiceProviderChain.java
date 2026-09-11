package uk.gov.dbt.ndtp.jena.abac.opa;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import uk.gov.dbt.ndtp.jena.abac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.abac.Hierarchy;
import uk.gov.dbt.ndtp.jena.abac.lib.CxtABAC;
import uk.gov.dbt.ndtp.jena.abac.opa.resilience.CachingDecisionServiceProvider;
import uk.gov.dbt.ndtp.jena.abac.opa.resilience.CircuitBreaker;
import uk.gov.dbt.ndtp.jena.abac.opa.resilience.CircuitBreakingDecisionServiceProvider;
import uk.gov.dbt.ndtp.jena.abac.opa.transport.OpaRequest;
import uk.gov.dbt.ndtp.jena.abac.opa.transport.OpaResponse;
import uk.gov.dbt.ndtp.jena.abac.opa.transport.OpaTransport;
import uk.gov.dbt.ndtp.jena.abac.opa.transport.OpaTransportException;

/**
 * Verifies the full decorator chain used in practice —
 * Caching -> CircuitBreaking -> Opa -> Transport — actually cooperates, not just
 * each class in isolation.
 */
public class TestDecisionServiceProviderChain {

    private static CxtABAC newCxt() {
        return CxtABAC.context(AttributeValueSet.of(java.util.List.of()), Hierarchy.noHierarchy, null);
    }

    /** Counts how many times the transport is actually called. */
    private static class CountingTransport implements OpaTransport {
        final AtomicInteger calls = new AtomicInteger(0);
        @Override
        public OpaResponse call(OpaRequest request, Duration connectTimeout, Duration readTimeout) {
            calls.incrementAndGet();
            return new OpaResponse(Set.of("public"), Map.of());
        }
    }

    private static DecisionServiceProvider fullChain(OpaTransport transport) {
        DecisionServiceProvider opa =
                new OpaDecisionServiceProvider(transport, Duration.ofSeconds(1), Duration.ofSeconds(1));
        DecisionServiceProvider circuitBreaking =
                new CircuitBreakingDecisionServiceProvider(opa, new CircuitBreaker(3, Duration.ofSeconds(30)));
        return new CachingDecisionServiceProvider(circuitBreaking);
    }

    @Test
    void secondCallWithSameCxt_hitsCacheNotTransport() {
        CountingTransport transport = new CountingTransport();
        DecisionServiceProvider chain = fullChain(transport);

        CxtABAC cxt = newCxt(); // ONE CxtABAC, as ABAC_Request.decideDataset() builds today
        DecisionContext ctx1 = new DecisionContext(cxt, "user-1", "read", "org-a", "dataset1");
        DecisionContext ctx2 = new DecisionContext(cxt, "user-1", "read", "org-a", "dataset1");

        DecisionResult first = chain.decide(ctx1, Set.of("public"));
        DecisionResult second = chain.decide(ctx2, Set.of("public"));

        assertEquals(first.permittedLabels(), second.permittedLabels());
        assertEquals(1, transport.calls.get(),
                "second call sharing the same CxtABAC must not reach the transport again");
    }

    @Test
    void differentCxt_doesNotShareCache() {
        CountingTransport transport = new CountingTransport();
        DecisionServiceProvider chain = fullChain(transport);

        DecisionContext ctx1 = new DecisionContext(newCxt(), "user-1", "read", "org-a", "dataset1");
        DecisionContext ctx2 = new DecisionContext(newCxt(), "user-1", "read", "org-a", "dataset1"); // different CxtABAC = different request

        chain.decide(ctx1, Set.of("public"));
        chain.decide(ctx2, Set.of("public"));

        assertEquals(2, transport.calls.get(),
                "two separate requests (separate CxtABAC) must each call the transport once");
    }

    @Test
    void circuitOpens_evenThroughCachingLayer() {
        OpaTransport failing = (request, ct, rt) -> {
            throw new OpaTransportException("boom");
        };
        DecisionServiceProvider opa =
                new OpaDecisionServiceProvider(failing, Duration.ofSeconds(1), Duration.ofSeconds(1));
        DecisionServiceProvider circuitBreaking =
                new CircuitBreakingDecisionServiceProvider(opa, new CircuitBreaker(1, Duration.ofSeconds(30)));
        DecisionServiceProvider chain = new CachingDecisionServiceProvider(circuitBreaking);

        // Each call uses its own CxtABAC (own request), so caching doesn't mask the
        // circuit-breaker behaviour underneath it.
        DecisionContext firstReq = new DecisionContext(newCxt(), "user-1", "read", "org-a", "dataset1");
        DecisionContext secondReq = new DecisionContext(newCxt(), "user-1", "read", "org-a", "dataset1");

        assertThrows(DecisionServiceUnavailableException.class, () -> chain.decide(firstReq, Set.of("public")));
        // Circuit now open (threshold 1) — second, unrelated request should fail fast too.
        assertThrows(DecisionServiceUnavailableException.class, () -> chain.decide(secondReq, Set.of("public")));
    }
}
