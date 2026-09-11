package uk.gov.dbt.ndtp.jena.abac.opa;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

import uk.gov.dbt.ndtp.jena.abac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.abac.Hierarchy;
import uk.gov.dbt.ndtp.jena.abac.lib.CxtABAC;
import uk.gov.dbt.ndtp.jena.abac.opa.transport.HttpOpaTransport;

/**
 * Manual, ad-hoc verification against a locally running OPA (docker run
 * openpolicyagent/opa:1.19.0, with the sag/test policy loaded - see project notes).
 * Not a JUnit test: requires a live OPA instance at localhost:8181, run by hand.
 * Covers: allow=true, allow=false, and unreachable OPA - the three cases that can only
 * be genuinely proven against a real network call, not the mock-based unit tests.
 */
public class TestHttpOpaTransportLive {

    public static void main(String[] args) throws Exception {
        scenarioAllowed();
        scenarioDenied();
        scenarioUnreachable();
        System.out.println("All live scenarios completed.");
    }

    /** User holding "employee" - expect a non-empty permitted set. */
    private static void scenarioAllowed() {
        DecisionResult result = decide("employee");
        System.out.println("allowed: permittedLabels=" + result.permittedLabels() + " isEmpty=" + result.isEmpty());
        if (result.isEmpty())
            throw new AssertionError("Expected a non-empty permitted set for 'employee'");
    }

    /** User without "employee" - expect an empty permitted set (real 403 case). */
    private static void scenarioDenied() {
        DecisionResult result = decide("contractor");
        System.out.println("denied: permittedLabels=" + result.permittedLabels() + " isEmpty=" + result.isEmpty());
        if (!result.isEmpty())
            throw new AssertionError("Expected an empty permitted set for 'contractor'");
    }

    /** Nothing listening on this port - expect DecisionServiceUnavailableException, not a hang or crash. */
    private static void scenarioUnreachable() {
        HttpOpaTransport transport = new HttpOpaTransport(
                URI.create("http://localhost:9999"), "sag/test", Duration.ofSeconds(1));
        OpaDecisionServiceProvider provider =
                new OpaDecisionServiceProvider(transport, Duration.ofSeconds(1), Duration.ofSeconds(1));

        try {
            provider.decide(context("employee"), Set.of("public", "employee", "military"));
            throw new AssertionError("Expected DecisionServiceUnavailableException, but call succeeded");
        } catch (DecisionServiceUnavailableException e) {
            System.out.println("unreachable: correctly threw " + e.getMessage());
        }
    }

    private static DecisionResult decide(String attribute) {
        HttpOpaTransport transport = new HttpOpaTransport(
                URI.create("http://localhost:8181"), "sag/test", Duration.ofSeconds(2));
        OpaDecisionServiceProvider provider =
                new OpaDecisionServiceProvider(transport, Duration.ofSeconds(2), Duration.ofSeconds(2));

        return provider.decide(context(attribute), Set.of("public", "employee", "military"));
    }

    private static DecisionContext context(String attribute) {
        CxtABAC cxt = CxtABAC.context(
                AttributeValueSet.of(attribute), Hierarchy.noHierarchy, null);
        return new DecisionContext(cxt, "user-1", "read", "org-a", "dataset1");
    }
}
