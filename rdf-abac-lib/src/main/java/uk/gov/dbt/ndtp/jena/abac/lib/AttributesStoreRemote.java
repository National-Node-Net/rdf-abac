// SPDX-License-Identifier: Apache-2.0
// Originally developed by Telicent Ltd.; subsequently adapted, enhanced, and maintained by the National Digital Twin Programme.
/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/*
 *  Modifications made by the National Digital Twin Programme (NDTP)
 *  © Crown Copyright 2025. This work has been developed by the National Digital Twin Programme
 *  and is legally attributed to the Department for Business and Trade (UK) as the governing entity.
 */


package uk.gov.dbt.ndtp.jena.abac.lib;

import uk.gov.dbt.ndtp.jena.abac.AE;
import uk.gov.dbt.ndtp.jena.abac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.abac.Hierarchy;
import uk.gov.dbt.ndtp.jena.abac.attributes.Attribute;
import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeSyntaxError;
import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeValue;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.jena.atlas.lib.StreamOps;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.http.HttpEnv;
import org.apache.jena.http.HttpLib;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.web.HttpNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.apache.jena.http.HttpLib.execute;
import static org.apache.jena.http.HttpLib.toRequestURI;

public class AttributesStoreRemote implements AttributesStore {

    private final Logger LOG = LoggerFactory.getLogger(AttributesStoreRemote.class);

    // [ABAC]
    // Awaiting update to remote user attribute store.
    // Will need a cache of user attributes
    // Will need a cache of hierarchies

    private final String userTemplate = "{user}";
    private final String lookupUserEndpoint;
    private final String hierarchyTemplate = "{name}";
    private final String lookupHierarchyEndpoint;
    private final HttpClient httpClient;

    /**
     * Creates a new remote user attributes store using a default HTTP Client from {@link HttpEnv#getDftHttpClient()}
     *
     * @param lookupUserEndpoint User attributes lookup endpoint
     * @param lookupHierarchyEndpoint Hierarchy lookup endpoint
     */
    public AttributesStoreRemote(String lookupUserEndpoint, String lookupHierarchyEndpoint) {
        this(lookupUserEndpoint, lookupHierarchyEndpoint, HttpEnv.getDftHttpClient());
    }

    /**
     * Creates a new remote user attributes store
     *
     * @param lookupUserEndpoint User attributes lookup endpoint. It should include the {@code {user}} template which is
     *                           substituted before each request with the requested user.
     * @param lookupHierarchyEndpoint Hierarchy lookup endpoint.  It should include the {@code {name}} template which is
     *                                substituted before each request with the requested hierarchy.
     * @param httpClient HTTP Client to use for requests to the remote store
     */
    public AttributesStoreRemote(String lookupUserEndpoint, String lookupHierarchyEndpoint, HttpClient httpClient) {
        if (lookupHierarchyEndpoint == null) {
            LOG.info("No hierarchy lookup service configured");
        }

        this.lookupUserEndpoint = lookupUserEndpoint;
        this.lookupHierarchyEndpoint = lookupHierarchyEndpoint;
        this.httpClient = Objects.requireNonNull(httpClient, "HTTP Client cannot be null");
        if (!lookupUserEndpoint.contains(userTemplate)) {
            LOG.warn("Endpoint does not contain `" + userTemplate + "`: " + lookupUserEndpoint);
        }
        if (lookupHierarchyEndpoint != null && !lookupHierarchyEndpoint.contains(hierarchyTemplate)) {
            LOG.warn("Endpoint does not contain `" + hierarchyTemplate + "`: " + lookupHierarchyEndpoint);
        }
    }

    /*
     * Get attributes from this {@code AttributeStore} for a given user.
     * The user must not be null.
     * <ul>
     * <li>null {@literal ->} unknown user.
     * <li>empty AttributeSet {@literal ->} known user, no attributes.
     */
    public static final String J_ATTRIBUTES = "attributes";

    // JsonString to string, with checking.
    private String jsonStringToString(JsonValue jvStr, JsonValue source) {
        if (!jvStr.isString()) {
            LOG.error("\"attributes\" element not a string: : " + JSON.toStringFlat(jvStr) + " in " + JSON.toStringFlat(
                    source));
            return null;
        }
        return jvStr.getAsString().value();
    }

    @Override
    public AttributeValueSet attributes(String userName) {
        String requestURL = A.substitute(lookupUserEndpoint, userTemplate, userName);
        FmtLog.info(LOG, "User attribute request: %s", requestURL);
        try {
            // Send: "/users/lookup/{user}"
            HttpRequest.Builder builder = HttpLib.requestBuilderFor(requestURL).uri(toRequestURI(requestURL)).GET();
            builder.setHeader(HttpNames.hAccept, WebContent.contentTypeJSON);
            HttpRequest request = builder.build();

            // Response
            //   { "attributes" : [ string1, string2 , ... ] }
            // each string is an attribute-value pair (with any necessary quoting within the string).
            //   See project https://github.com/National-Node-Net/rdf-abac/tree/main/docs/abac-specification.md
            //   See ?? for the network API.
            HttpResponse<InputStream> response = execute(httpClient, request);
            InputStream in = response.body();
            if (response.statusCode() == 404) {
                String x = IO.readWholeFileAsUTF8(in);
                FmtLog.warn(LOG, "Response from remote attribute store : 404\n%s", x);
                return null;
            }

            JsonValue jv = JSON.parseAny(in);
            if (!jv.isObject()) {
                LOG.error("Response from remote attribute store is not a JSON object: " + JSON.toStringFlat(jv));
                return null;
            }

            JsonValue jva = jv.getAsObject().get(J_ATTRIBUTES);


            if (jva == null) {
                LOG.error(
                        "Response from remote attribute store does not contain \"" + J_ATTRIBUTES + "\" field: " + JSON.toStringFlat(
                                jv));
                return null;
            }

            if (!jva.isArray()) {
                LOG.error("\"" + J_ATTRIBUTES + "\" is not a JSON array: " + JSON.toStringFlat(jva));
                return null;
            }

            FmtLog.info(LOG, "Received (%s): %s", userName, JSON.toStringFlat(jva));

            // Expected: JSON:
            //     { ...
            //       attributes: [ "attribute-value", "attribute-value", "attribute-value"],
            //       ...
            //     }

            List<String> s = jva.getAsArray().stream()
                                .map(a -> jsonStringToString(a, jva))
                                .filter(Objects::nonNull)
                                .toList();

            try {
                return parseResponse.apply(s.stream());
            } catch (AttributeSyntaxError ex) {
                FmtLog.info(LOG, "AttributeSyntaxError in response: %s. Response = |%s|", ex.getMessage(),
                            JSON.toStringFlat(jva));
                throw ex;
            }
        } catch (HttpException ex) {
            LOG.error("HttpException", ex);
            return null;
        }
    }

    private static final Function<Stream<String>, AttributeValueSet> parseResponse = (Stream<String> items) -> {
        Stream<AttributeValue> s2 = items.map(AE::parseAttrValue);
        List<AttributeValue> attrValues = StreamOps.toList(s2);
        return AttributeValueSet.of(attrValues);
    };

    @Override
    public boolean hasHierarchy(Attribute attribute) {
        Hierarchy hierarchy = getHierarchy(attribute);
        return hierarchy != null && !hierarchy.values().isEmpty();
    }

    public static final String J_HIERARCHY_LEVELS_1 = "tiers";
    public static final String J_HIERARCHY_LEVELS_2 = "levels";

    @Override
    public Hierarchy getHierarchy(Attribute attribute) {
        if (lookupHierarchyEndpoint == null) {
            return null;
        }

        String requestURL = A.substitute(lookupHierarchyEndpoint, hierarchyTemplate, attribute.name());
        LOG.info("Hierarchy lookup request: " + requestURL);
        try {
            HttpRequest.Builder builder = HttpLib.requestBuilderFor(requestURL).uri(toRequestURI(requestURL)).GET();
            builder.setHeader(HttpNames.hAccept, WebContent.contentTypeJSON);
            HttpRequest request = builder.build();
            HttpResponse<InputStream> response = execute(httpClient, request);
            InputStream in = response.body();
            if (response.statusCode() == 404) {
                String x = IO.readWholeFileAsUTF8(in);
                LOG.warn("Response from remote attribute store : 404\n" + x);
                return null;
            }

            JsonValue jv = JSON.parseAny(in);
            if (!jv.isObject()) {
                LOG.error("Response from remote attribute store is not a JSON object: " + JSON.toStringFlat(jv));
                return null;
            }

            // Response
            // { "uuid": "...." ,
            //   "name": "...." ,
            //   "tiers" : [ string , string, ]"
            // }
            //

            JsonValue jva = getFromJsonObject(jv.getAsObject(), J_HIERARCHY_LEVELS_1, J_HIERARCHY_LEVELS_2);
            if (jva == null) {
                LOG.info("Response: no such hierarchy: " + attribute.name());
                return null;
            }

            if (!jva.isArray()) {
                LOG.error("\"" + J_HIERARCHY_LEVELS_1 + "\" is not a JSON array: " + JSON.toStringFlat(jva));
                return null;
            }

            List<String> levels = jva.getAsArray().stream()
                                     .map(a -> jsonStringToString(a, jva))
                                     .filter(Objects::nonNull)
                                     .toList();

            Hierarchy hierarchy = Hierarchy.create(attribute, levels);
            LOG.info("Response: " + hierarchy);
            return hierarchy;
        } catch (HttpException ex) {
            LOG.error("HttpException", ex);
            return null;
        }
    }

    /**
     * Get a JSON value from a field with different possible names
     */
    static JsonValue getFromJsonObject(JsonObject jObj, String... fieldNames) {
        for (String name : fieldNames) {
            JsonValue jv = jObj.get(name);
            if (jv != null) {
                return jv;
            }
        }
        return null;
    }

    @Override
    public Set<String> users() {
        return Set.of();
    }
}
