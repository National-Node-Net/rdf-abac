package uk.gov.dbt.ndtp.jena.abac.opa;

import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeValue;
import uk.gov.dbt.ndtp.jena.abac.lib.CxtABAC;
import uk.gov.dbt.ndtp.jena.abac.opa.transport.*;

/**
 * OPA-backed {@link DecisionServiceProvider}. Everything except the wire format (owned by
 * {@link OpaTransport}) is implemented here: explicit connect/read timeouts (AC8), the
 * fail-closed mapping on error (AC9), and the defensive intersection against the submitted
 * vocabulary so a label OPA didn't see can never come back permitted (AC7).
 * <p>
 * Confirmed: SAG resolves and sends the subject's full attribute set to OPA -
 * see {@link #extractSubjectAttributes} - rather than OPA calling back to the PIP.
 */
public class OpaDecisionServiceProvider implements DecisionServiceProvider {

    private final OpaTransport transport;
    private final Duration connectTimeout;
    private final Duration readTimeout;

    public OpaDecisionServiceProvider(OpaTransport transport, Duration connectTimeout, Duration readTimeout) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.connectTimeout = Objects.requireNonNull(connectTimeout, "connectTimeout");
        this.readTimeout = Objects.requireNonNull(readTimeout, "readTimeout");
    }

    @Override
    public DecisionResult decide(DecisionContext context, Set<String> vocabulary) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(vocabulary, "vocabulary");

        if ( vocabulary.isEmpty() )
            return DecisionResult.empty();

        Set<String> subjectAttributes = extractSubjectAttributes(context.cxt());
        OpaRequest request = OpaRequest.of(context, vocabulary, subjectAttributes);

        OpaResponse response;
        try {
            response = transport.call(request, connectTimeout, readTimeout);
        } catch (OpaConnectException e) {
            throw new DecisionServiceUnavailableException("OPA unreachable", e);
        } catch (OpaTransportException e) {
            // timeout / HTTP error / malformed body all map to 503,
            // not to an empty permitted set. Only a genuine HTTP 200 with an empty
            // permitted set is a 403. Matches opa-pov's OpaPolicyDecisionPoint behaviour too
            // (non-2xx and undefined-rule responses both raise, never silently deny).
            throw new DecisionServiceUnavailableException("OPA transport failure", e);
        }

        if ( response == null || response.permittedLabels() == null ) {
            throw new DecisionServiceUnavailableException("OPA returned a malformed response");
        }

        Set<String> permitted = new HashSet<>(response.permittedLabels());
        permitted.retainAll(vocabulary);

        return new DecisionResult(permitted, response.auditMetadata());
    }

    /**
     * Flattens the subject's resolved attributes into the same "name" / "name=value" string
     * form used by data labels. Confirmed: sent raw, never hierarchy-expanded - Rego
     * does the hierarchy comparison on its side ("no decision making happens in SAG").
     */
    private static Set<String> extractSubjectAttributes(CxtABAC cxt) {
        Set<String> attributes = new HashSet<>();
        cxt.requestAttributes().attributeValues((AttributeValue av) -> {
            String name = av.attribute().name();
            String value = av.value().asString();
            attributes.add("true".equals(value) ? name : name + "=" + value);
        });
        return attributes;
    }
}
