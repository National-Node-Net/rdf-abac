package uk.gov.dbt.ndtp.jena.abac.opa;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import uk.gov.dbt.ndtp.jena.abac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.abac.Hierarchy;
import uk.gov.dbt.ndtp.jena.abac.lib.CxtABAC;
import uk.gov.dbt.ndtp.jena.abac.opa.transport.*;

public class TestOpaDecisionServiceProvider {

    private static DecisionContext ctx() {
        CxtABAC cxt = CxtABAC.context(AttributeValueSet.of(java.util.List.of()), Hierarchy.noHierarchy, null);
        return new DecisionContext(cxt, "user-1", "read", "org-a", "dataset1");
    }

    /** Test double: each test configures what the transport does. */
    private static class FakeTransport implements OpaTransport {
        interface Behaviour {
            OpaResponse call() throws OpaConnectException, OpaTransportException;
        }
        private final Behaviour behaviour;
        FakeTransport(Behaviour behaviour) { this.behaviour = behaviour; }

        @Override
        public OpaResponse call(OpaRequest request, Duration connectTimeout, Duration readTimeout)
                throws OpaConnectException, OpaTransportException {
            return behaviour.call();
        }
    }

    @Test
    void decide_success_returnsPermittedIntersectedWithVocabulary() {
        OpaTransport transport = new FakeTransport(() ->
                new OpaResponse(Set.of("public", "employee"), Map.of("policyId", "v1")));
        OpaDecisionServiceProvider provider =
                new OpaDecisionServiceProvider(transport, Duration.ofSeconds(1), Duration.ofSeconds(1));

        DecisionResult result = provider.decide(ctx(), Set.of("public", "employee", "military"));

        assertEquals(Set.of("public", "employee"), result.permittedLabels());
    }

    @Test
    void decide_connectFailure_throwsUnavailable() {
        OpaTransport transport = new FakeTransport(() -> {
            throw new OpaConnectException("refused", null);
        });
        OpaDecisionServiceProvider provider =
                new OpaDecisionServiceProvider(transport, Duration.ofSeconds(1), Duration.ofSeconds(1));

        assertThrows(DecisionServiceUnavailableException.class,
                () -> provider.decide(ctx(), Set.of("public")));
    }

    @Test
    void decide_readTimeout_throwsUnavailable() {
        // OPA reachable but not responding in time is NOT a
        // silent deny - it's the same "can't trust the answer" bucket as connection
        // failure, and maps to 503 downstream (SAG-05), not 403.
        OpaTransport transport = new FakeTransport(() -> {
            throw new OpaTransportException("read timed out");
        });
        OpaDecisionServiceProvider provider =
                new OpaDecisionServiceProvider(transport, Duration.ofSeconds(1), Duration.ofSeconds(1));

        assertThrows(DecisionServiceUnavailableException.class,
                () -> provider.decide(ctx(), Set.of("public")));
    }

    @Test
    void decide_http500_throwsUnavailable() {
        // Same reasoning as the timeout case above - an HTTP error response from OPA
        // is not a policy decision, so it can't be folded into an empty permitted set.
        OpaTransport transport = new FakeTransport(() -> {
            throw new OpaTransportException("HTTP 500");
        });
        OpaDecisionServiceProvider provider =
                new OpaDecisionServiceProvider(transport, Duration.ofSeconds(1), Duration.ofSeconds(1));

        assertThrows(DecisionServiceUnavailableException.class,
                () -> provider.decide(ctx(), Set.of("public")));
    }

    @Test
    void decide_malformedResponse_throwsUnavailable() {
        // A response OPA sent but that can't be parsed is untrustworthy, same as the
        // cases above - not a genuine "permit nothing" decision from OPA.
        OpaTransport transport = new FakeTransport(() -> new OpaResponse(null, null));
        OpaDecisionServiceProvider provider =
                new OpaDecisionServiceProvider(transport, Duration.ofSeconds(1), Duration.ofSeconds(1));

        assertThrows(DecisionServiceUnavailableException.class,
                () -> provider.decide(ctx(), Set.of("public")));
    }

    @Test
    void decide_emptyVocabulary_neverCallsTransport() {
        OpaTransport transport = new FakeTransport(() -> {
            fail("transport should not be called for an empty vocabulary");
            return null;
        });
        OpaDecisionServiceProvider provider =
                new OpaDecisionServiceProvider(transport, Duration.ofSeconds(1), Duration.ofSeconds(1));

        DecisionResult result = provider.decide(ctx(), Set.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void decide_labelAbsentFromVocabulary_neverPermitted() {
        OpaTransport transport = new FakeTransport(() ->
                new OpaResponse(Set.of("public", "military"), Map.of()));
        OpaDecisionServiceProvider provider =
                new OpaDecisionServiceProvider(transport, Duration.ofSeconds(1), Duration.ofSeconds(1));

        DecisionResult result = provider.decide(ctx(), Set.of("public"));

        assertEquals(Set.of("public"), result.permittedLabels());
    }

    @Test
    void decide_genuineEmptyPermittedSet_isNotAnError() {
        // The one and only case that should map to a 403 downstream: OPA responded
        // successfully (HTTP 200, well-formed body) and explicitly permitted nothing.
        OpaTransport transport = new FakeTransport(() ->
                new OpaResponse(Set.of(), Map.of("reason", "no matching policy")));
        OpaDecisionServiceProvider provider =
                new OpaDecisionServiceProvider(transport, Duration.ofSeconds(1), Duration.ofSeconds(1));

        DecisionResult result = provider.decide(ctx(), Set.of("public"));

        assertTrue(result.isEmpty());
    }
}