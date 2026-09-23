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


package uk.gov.dbt.ndtp.platform.play;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.apache.jena.atlas.io.AWriter;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.http.HttpEnv;
import org.apache.jena.http.HttpLib;
import org.apache.jena.http.Push;
import org.apache.jena.http.RegistryHttpClient;
import org.apache.jena.http.auth.AuthEnv;

/**
 * Packaged-up operations to play a file over HTTP.
 * This uses {@code org.apache.jena.http} and authentication support for basic auth is available
 * {@link AuthEnv#registerUsernamePassword} as is {@link RegistryHttpClient} for using custom
 * {@link java.net.http.HttpClient}s.
 */
public class PlayHTTP {
    /** POST a message file contents to URL - HTTP headers taken from the file. */
    static public void sendFileHTTP(String url, String filename) {
        httpRequestResponse(url, filename);
    }

//    /** POST a string in message file format to URL - HTTP headers taken from the string. */
//    static public void sendStringHTTP(String url, String string) {
//        InputStream input = new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
//        String x = httpRequestResponse(url, input, true);
//    }
//
//    /** Send bytes in message format from an {@link InputStream} to a URL. */
//    static public void sendHTTP(String url, InputStream input) {
//        httpRequestResponse(url, input, false);
//    }

    /** HTTP request, no response body */
    private static void httpRequestResponse(String url, String filename) {
        MessageRequest message = PlayFiles.fileToMessageRequest(Path.of(filename), null);
        httpRequestResponse(url, message, false);
    }

    /** HTTP request as a {@link MessageRequest}, return the response body as a string. */
    private static String httpRequestResponse(String url, MessageRequest message, boolean withResponse) {
        PlaySenderHTTP sender = new PlaySenderHTTP(url);
        BodyPublisher bodyPublisher = BodyPublishers.ofInputStream(()->message.getBody());
        Consumer<HttpRequest.Builder> modifier = (builder) -> message.getHeaders().forEach((h,v) -> builder.header(h, v));
        if ( ! withResponse ) {
            // If not requirement for a response.
            // The return body will be consumed to keep the connection state correct.
            HttpLib.httpPushData(HttpEnv.getDftHttpClient(), Push.POST, url, modifier, bodyPublisher);
            return null;
        }
        // With response.
        HttpResponse<InputStream> response = httpPushWithResponse(HttpEnv.getDftHttpClient(), Push.POST, url, modifier, bodyPublisher);
        HttpLib.handleHttpStatusCode(response);
        StringWriter w = new StringWriter();
        try ( AWriter out = IO.wrap(w) ) {
            printResponse(out, response);
        }
        // Consume all to keep the connection happy.
        HttpLib.finish(response.body());
        return w.toString();
    }

    // From Jena HttpLib (where it is not public)
    /**
     * Engine to PUT/POST/PATCH a request.
     * <p>
     * The request body comes from a {@link BodyPublisher}
     * headers can be set by the optional modification set.
     * The response has not been check for status code - use {@link HttpLib#handleHttpStatusCode}.
     */
    public static HttpResponse<InputStream> httpPushWithResponse(HttpClient httpClient, Push style, String url,
                                                                 Consumer<HttpRequest.Builder> modifier, BodyPublisher body) {
        URI uri = HttpLib.toRequestURI(url);
        HttpRequest.Builder builder = HttpLib.requestBuilderFor(url);
        builder.uri(uri);
        builder.method(style.method(), body);
        if ( modifier != null )
            modifier.accept(builder);
        HttpResponse<InputStream> response = HttpLib.execute(httpClient, builder.build());
        return response;
    }

    /** Write in message format. */
    private static void printResponse(AWriter out, HttpResponse<InputStream> response) {
        // Write response.
        response.headers().map().forEach((header,values)->{
            values.forEach(v->out.printf("%s: %s\n", header, v));
        });
        out.println();
        String x = IO.readWholeFileAsUTF8(response.body());
        if ( ! x.isEmpty() )
            out.print(x);
    }
}
