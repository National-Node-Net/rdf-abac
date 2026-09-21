package uk.gov.dbt.ndtp.jena.abac.opa.transport;

import java.time.Duration;

/**
 * Seam for the actual OPA wire call. Deliberately abstract: the request/response JSON
 * shape is owned by OPA team and hasn't landed yet (as of Aug 2026). Implement
 * this once the contract exists. {@link OpaRequest} and {@link OpaResponse} carry only
 * what SAG-04's acceptance criteria require today, so the rest of the provider - timeouts,
 * fail-closed mapping, the vocabulary intersection - can be built and tested against them
 * now, independent of the wire format.
 */
public interface OpaTransport {
    OpaResponse call(OpaRequest request, Duration connectTimeout, Duration readTimeout)
            throws OpaConnectException, OpaTransportException;
}
