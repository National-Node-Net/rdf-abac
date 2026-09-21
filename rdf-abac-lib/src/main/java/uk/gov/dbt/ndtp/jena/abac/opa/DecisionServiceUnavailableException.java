package uk.gov.dbt.ndtp.jena.abac.opa;

/**
 * Thrown when OPA cannot be reached at all - connection refused, DNS failure, connect
 * timeout - or when OPA responded but the response can't be trusted: HTTP error status,
 * read timeout, or a malformed body. Confirmed with Kacper: any of these map to 503
 * downstream (SAG-05), distinct from a genuine policy denial (an empty
 * {@link DecisionResult} from a well-formed HTTP 200 response), which maps to 403.
 */
public class DecisionServiceUnavailableException extends RuntimeException {
    public DecisionServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
    public DecisionServiceUnavailableException(String message) {
        super(message);
    }
}
