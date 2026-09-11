package uk.gov.dbt.ndtp.jena.abac.opa.transport;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonBuilder;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.web.HttpNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dbt.ndtp.jena.abac.opa.OpaDecisionServiceProvider;

import static org.apache.jena.http.HttpLib.execute;
import static org.apache.jena.http.HttpLib.toRequestURI;

/**
 * Real HTTP implementation of {@link OpaTransport}, calling a local/deployed OPA instance
 * over its Data API - {@code POST <base>/v1/data/<policyPackage>/allow}, body
 * {@code {"input": {...}}}, response {@code {"result": true|false}}.
 * <p>
 * Confirmed against a locally-run OPA (docker, {@code openpolicyagent/opa:1.19.0}) with a
 * hand-written test policy, per OPA team guidance - the endpoint shape and request/response
 * envelope are the standard OPA Data API, not something SAG-specific.
 * <p>
 * The response document must carry an explicit {@code allow} boolean - not something
 * derived purely from an empty permitted set on our side. This class reads that field;
 * {@link OpaDecisionServiceProvider} still performs its own intersection against the
 * submitted vocabulary regardless of what {@code allow} says - a label OPA didn't ask
 * about is never trusted.
 * <p>
 * Uses its own dedicated {@link HttpClient} rather than the shared
 * {@code HttpEnv.getDftHttpClient()} used elsewhere in this codebase (e.g.
 * {@link uk.gov.dbt.ndtp.jena.abac.lib.AttributesStoreRemote}) - deliberately, because
 * connect/read timeouts need to be configurable per provider instance, and the JDK
 * {@code HttpClient}'s connect timeout is fixed at client-build time, not per-request.
 * <p>
 * Field names ({@link #FIELD_ALLOW} etc.), the wrapping envelope, and the policy path are
 * all provisional pending the final contract - kept as named constants so the eventual
 * rename touches one place, not every call site.
 */
public class HttpOpaTransport implements OpaTransport {

    private static final Logger LOG = LoggerFactory.getLogger(HttpOpaTransport.class);

    // Request field names - TODO: confirm with OPA team final contract.
    private static final String FIELD_SUBJECT_ID = "subject_id";
    private static final String FIELD_ACTION = "action";
    private static final String FIELD_ORGANISATION_ID = "organisation_id";
    private static final String FIELD_DATASET_NAME = "dataset_name";
    private static final String FIELD_VOCABULARY = "vocabulary";
    private static final String FIELD_SUBJECT_ATTRIBUTES = "subject_attributes";
    private static final String FIELD_INPUT_ENVELOPE = "input";

    // Response field names - TODO: confirm with OPA team final contract.
    private static final String FIELD_ALLOW = "allow";
    private static final String FIELD_PERMITTED_LABELS = "permitted_labels";
    private static final String FIELD_RESULT_ENVELOPE = "result";

    // TODO: confirm final policy path with OPA team - today points at the hand-written
    // test policy used to prove the chain end-to-end.
    private final URI decisionUri;
    private final HttpClient httpClient;

    public HttpOpaTransport(URI opaBaseUri, String policyPackagePath, Duration connectTimeout) {
        Objects.requireNonNull(opaBaseUri, "opaBaseUri");
        Objects.requireNonNull(policyPackagePath, "policyPackagePath");
        this.decisionUri = opaBaseUri.resolve("/v1/data/" + policyPackagePath);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Objects.requireNonNull(connectTimeout, "connectTimeout"))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public OpaResponse call(OpaRequest request, Duration connectTimeout, Duration readTimeout)
            throws OpaConnectException, OpaTransportException {
        String body = toJson(request);
        FmtLog.debug(LOG, "OPA request: %s", body);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(toRequestURI(decisionUri.toString()))
                .timeout(readTimeout)
                .header(HttpNames.hContentType, WebContent.contentTypeJSON)
                .header(HttpNames.hAccept, WebContent.contentTypeJSON)
                .POST(BodyPublishers.ofString(body))
                .build();

        HttpResponse<InputStream> response;
        try {
            response = execute(httpClient, httpRequest);
        } catch (HttpException ex) {
            throw new OpaTransportException("OPA call failed for " + decisionUri, ex);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String errorBody = IO.readWholeFileAsUTF8(response.body());
            throw new OpaTransportException(
                    "OPA returned HTTP " + response.statusCode() + " for " + decisionUri + ": " + errorBody);
        }

        JsonValue parsed;
        try {
            parsed = JSON.parseAny(response.body());
        } catch (RuntimeException ex) {
            throw new OpaTransportException("OPA response was not valid JSON", ex);
        }

        return parseResponse(parsed);
    }

    public static String toJson(OpaRequest request) {
        JsonBuilder builder = new JsonBuilder();
        builder.startObject()
                .key(FIELD_INPUT_ENVELOPE).startObject()
                .key(FIELD_SUBJECT_ID).value(request.subjectId())
                .key(FIELD_ACTION).value(request.action())
                .key(FIELD_ORGANISATION_ID);
        if (request.organisationId() == null)
            builder.valueNull();
        else
            builder.value(request.organisationId());

        builder.key(FIELD_DATASET_NAME).value(request.datasetName())
                .key(FIELD_VOCABULARY).startArray();
        for (String v : request.vocabulary())
            builder.value(v);
        builder.finishArray()
                .key(FIELD_SUBJECT_ATTRIBUTES).startArray();
        for (String v : request.subjectAttributes())
            builder.value(v);
        builder.finishArray()
                .finishObject()
                .finishObject();

        return JSON.toStringFlat(builder.build());
    }

    public static OpaResponse parseResponse(JsonValue parsed) throws OpaTransportException {
        JsonValue document = parsed.isObject() && parsed.getAsObject().hasKey(FIELD_RESULT_ENVELOPE)
                ? parsed.getAsObject().get(FIELD_RESULT_ENVELOPE)
                : parsed;

        if (!document.isObject())
            throw new OpaTransportException("OPA response is not a JSON object: " + JSON.toStringFlat(parsed));

        JsonObject obj = document.getAsObject();

        if (!obj.hasKey(FIELD_ALLOW) || !obj.get(FIELD_ALLOW).isBoolean())
            throw new OpaTransportException("OPA response missing required '" + FIELD_ALLOW + "' field: " + JSON.toStringFlat(parsed));

        boolean allow = obj.get(FIELD_ALLOW).getAsBoolean().value();

        Set<String> permitted = new HashSet<>();
        if (allow) {
            if (!obj.hasKey(FIELD_PERMITTED_LABELS) || !obj.get(FIELD_PERMITTED_LABELS).isArray())
                throw new OpaTransportException(
                        "OPA allowed but '" + FIELD_PERMITTED_LABELS + "' is missing or malformed: " + JSON.toStringFlat(parsed));
            for (JsonValue v : obj.get(FIELD_PERMITTED_LABELS).getAsArray())
                if (v.isString())
                    permitted.add(v.getAsString().value());
        }

        return new OpaResponse(permitted, java.util.Map.of());
    }
}
