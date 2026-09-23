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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.atlas.io.AWriter;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.http.HttpEnv;
import org.apache.jena.http.HttpLib;
import org.apache.jena.http.Push;

public class PlaySenderHTTP implements PlaySender {

    private String destinationURL;

    public PlaySenderHTTP(String destinationURL) {
        this.destinationURL = destinationURL;
    }

    @Override
    public void play(Map<String, String> headers, InputStream input) {
        httpRequestNoResponse(destinationURL, headers, input);
    }

    /** HTTP request-response. Send some bytes, not expecting a response. */
    private static void httpRequestNoResponse(String url, Map<String, String> headers, InputStream input) {
        try ( input ) {
            BodyPublisher bodyPublisher = BodyPublishers.ofInputStream(()->input);
            Consumer<HttpRequest.Builder> modifier = builder -> headers.forEach((h,v) -> builder.header(h, v));
            HttpLib.httpPushData(HttpEnv.getDftHttpClient(), Push.POST, url, modifier, bodyPublisher);
        } catch (IOException e) {
            throw new RuntimeIOException(e.getMessage());
        }
    }

    // -- With response.

    /** HTTP request-response. Send some bytes, get a response as a string. */
    private static String httpRequestResponse(String url, Map<String, String> headers, InputStream input) {
        try ( input ) {
            BodyPublisher bodyPublisher = BodyPublishers.ofInputStream(()->input);
            Consumer<HttpRequest.Builder> modifier = builder -> headers.forEach((h,v) -> builder.header(h, v));
            HttpResponse<InputStream> response = httpPushWithResponse(HttpEnv.getDftHttpClient(), Push.POST, url, modifier, bodyPublisher);
            HttpLib.handleHttpStatusCode(response);
            StringWriter w = new StringWriter();
            try ( AWriter out = IO.wrap(w) ) {
                printResponse(out, response);
            }
            // Consume all to keep the connection happy.
            HttpLib.finish(response.body());
            return w.toString();
        } catch (IOException e) {
            throw new RuntimeIOException(e.getMessage());
        }

    }

    // From Jena HttpLib (where it is not public)
    /**
     * Engine to PUT/POST/PATCH a request.
     * <p>
     * The request body comes from a {@link BodyPublisher}
     * headers can be set by the optional modification set.
     * The response has not been check for status code - use {@link HttpLib#handleHttpStatusCode}.
     */
    private static HttpResponse<InputStream> httpPushWithResponse(HttpClient httpClient, Push style, String url,
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
    private static void printResponse(AWriter out, HttpResponse<InputStream> response) throws IOException {
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
