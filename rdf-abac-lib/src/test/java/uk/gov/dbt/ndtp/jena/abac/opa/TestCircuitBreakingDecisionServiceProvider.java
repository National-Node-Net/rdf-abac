package uk.gov.dbt.ndtp.jena.abac.opa;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import uk.gov.dbt.ndtp.jena.abac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.abac.Hierarchy;
import uk.gov.dbt.ndtp.jena.abac.lib.CxtABAC;
import uk.gov.dbt.ndtp.jena.abac.opa.resilience.CircuitBreaker;
import uk.gov.dbt.ndtp.jena.abac.opa.resilience.CircuitBreakingDecisionServiceProvider;

public class TestCircuitBreakingDecisionServiceProvider {

    private static DecisionContext ctx() {
        CxtABAC cxt = CxtABAC.context(AttributeValueSet.of(java.util.List.of()), Hierarchy.noHierarchy, null);
        return new DecisionContext(cxt, "user-1", "read", "org-a", "dataset1");
    }

    private static class StubProvider implements DecisionServiceProvider {
        interface Behaviour { DecisionResult decide(); }
        private final Behaviour behaviour;
        StubProvider(Behaviour behaviour) { this.behaviour = behaviour; }
        @Override public DecisionResult decide(DecisionContext c, Set<String> v) { return behaviour.decide(); }
    }

    @Test
    void opensCircuitAfterRepeatedUnavailability() {
        DecisionServiceProvider failing = new StubProvider(() -> {
            throw new DecisionServiceUnavailableException("down");
        });
        CircuitBreakingDecisionServiceProvider provider =
                new CircuitBreakingDecisionServiceProvider(failing, new CircuitBreaker(2, Duration.ofSeconds(30)));

        assertThrows(DecisionServiceUnavailableException.class, () -> provider.decide(ctx(), Set.of("public")));
        assertThrows(DecisionServiceUnavailableException.class, () -> provider.decide(ctx(), Set.of("public")));
        assertThrows(DecisionServiceUnavailableException.class, () -> provider.decide(ctx(), Set.of("public")));
    }

    @Test
    void emptyResultDoesNotOpenCircuit() {
        DecisionServiceProvider deniesEverything = new StubProvider(DecisionResult::empty);
        CircuitBreakingDecisionServiceProvider provider =
                new CircuitBreakingDecisionServiceProvider(deniesEverything, new CircuitBreaker(1, Duration.ofSeconds(30)));

        provider.decide(ctx(), Set.of("public"));
        DecisionResult result = provider.decide(ctx(), Set.of("public"));

        assertTrue(result.isEmpty());
    }

    @Test
    void successPassesThroughResult() {
        DecisionServiceProvider ok = new StubProvider(() -> new DecisionResult(Set.of("public"), java.util.Map.of()));
        CircuitBreakingDecisionServiceProvider provider =
                new CircuitBreakingDecisionServiceProvider(ok, new CircuitBreaker(3, Duration.ofSeconds(30)));

        DecisionResult result = provider.decide(ctx(), Set.of("public"));

        assertEquals(Set.of("public"), result.permittedLabels());
    }
}
