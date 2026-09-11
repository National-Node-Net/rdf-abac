package uk.gov.dbt.ndtp.jena.abac.opa.transport;

/** HTTP-level failure: non-2xx response, read timeout, malformed body. */
public class OpaTransportException extends Exception {
    public OpaTransportException(String message, Throwable cause) { super(message, cause); }
    public OpaTransportException(String message) { super(message); }
}