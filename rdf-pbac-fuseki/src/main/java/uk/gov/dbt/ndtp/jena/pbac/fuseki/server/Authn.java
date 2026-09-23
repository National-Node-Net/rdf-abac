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


package uk.gov.dbt.ndtp.jena.pbac.fuseki.server;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

import org.apache.jena.atlas.lib.Bytes;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.http.HttpLib;
import org.apache.jena.riot.web.HttpNames;

public class Authn {

    //Authorization: Bearer <token>
    public static String AUTH_HEADER = HttpNames.hAuthorization;

    public static String BEARER = "Bearer";
    public static String BEARER_PREFIX = "Bearer ";
    public static String USER_PREFIX = "user:";

    /** Get the user name from a "user:" bearer token which is base64 encoded. */
    public static String getUserFromToken64(String token) {
        byte[] bytes = Base64.getDecoder().decode(token);
        String string = Bytes.bytes2string(bytes);
        return getUserFromString(string);
    }

    /** Get the user name from a "user:" bearer token which is in plain text (development). */
    public static String getUserFromString(String decodedString) {
        if ( ! decodedString.startsWith(USER_PREFIX) ) {
            Log.info(Authn.class, "Bearer token does not start \"user:\"");
            return null;
        }
        String user = decodedString.substring(USER_PREFIX.length());
        return user;
    }


    /** Create the header string setting for bear auth in the "user:" scheme. */
    public static String requestAuthorizationHeader(String user) {
        Objects.requireNonNull(user);
        String x = "user:"+user;
        String x64 = Base64.getEncoder().encodeToString(x.getBytes(StandardCharsets.UTF_8));
        return HttpLib.bearerAuthHeader(x64);
    }
}
