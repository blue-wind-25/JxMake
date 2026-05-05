/*
 * ##### This file has been modified by JxMake project #####
 */

/*
 * MIT License
 *
 * Copyright (c) 2022 IntellectualSites
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.intellectualsites.http;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*
 {@link HttpExecutor} implementation that delegates to {@code java.net.http.HttpClient}
 entirely through reflection so that this source file can be compiled on Java 8 without
 importing any {@code java.net.http.*} type directly.

 All {@code java.net.http} classes and the methods used on them are resolved once in the
 static initialiser and stored in {@code static final} fields.  A single
 {@code java.net.http.HttpClient} instance is created at that point and reused for every
 request; {@code HttpClient} is documented as thread-safe.
 */
final class HttpClientExecutor implements HttpExecutor {

    /* ------------------------------------------------------------------ *
     *  Reflectively resolved classes                                       *
     * ------------------------------------------------------------------ */

    private static final Class<?> CLS_HTTP_CLIENT;
    private static final Class<?> CLS_HTTP_REQUEST;
    private static final Class<?> CLS_HTTP_REQUEST_BUILDER;
    private static final Class<?> CLS_BODY_PUBLISHERS;
    private static final Class<?> CLS_BODY_PUBLISHER;
    private static final Class<?> CLS_HTTP_RESPONSE;
    private static final Class<?> CLS_BODY_HANDLERS;
    private static final Class<?> CLS_BODY_HANDLER;
    private static final Class<?> CLS_HTTP_HEADERS;
    private static final Class<?> CLS_DURATION;

    /* ------------------------------------------------------------------ *
     *  Reflectively resolved methods                                       *
     * ------------------------------------------------------------------ */

    private static final Method M_NEW_BUILDER;
    private static final Method M_BUILDER_URI;
    private static final Method M_BUILDER_METHOD;
    private static final Method M_BUILDER_HEADER;
    private static final Method M_BUILDER_TIMEOUT;
    private static final Method M_BUILDER_BUILD;
    private static final Method M_PUBLISHERS_OF_BYTE_ARRAY;
    private static final Method M_PUBLISHERS_NO_BODY;
    private static final Method M_HANDLERS_OF_BYTE_ARRAY;
    private static final Method M_CLIENT_SEND;
    private static final Method M_RESPONSE_STATUS_CODE;
    private static final Method M_RESPONSE_BODY;
    private static final Method M_RESPONSE_HEADERS;
    private static final Method M_HEADERS_MAP;
    private static final Method M_DURATION_OF_MILLIS;

    /* ------------------------------------------------------------------ *
     *  Shared HttpClient instance                                          *
     * ------------------------------------------------------------------ */

    private static final Object CLIENT_INSTANCE;

    /* ------------------------------------------------------------------ *
     *  Static initialiser – runs exactly once                              *
     * ------------------------------------------------------------------ */

    static {
        try {
            CLS_HTTP_CLIENT          = Class.forName("java.net.http.HttpClient");
            CLS_HTTP_REQUEST         = Class.forName("java.net.http.HttpRequest");
            CLS_HTTP_REQUEST_BUILDER = Class.forName("java.net.http.HttpRequest$Builder");
            CLS_BODY_PUBLISHERS      = Class.forName("java.net.http.HttpRequest$BodyPublishers");
            CLS_BODY_PUBLISHER       = Class.forName("java.net.http.HttpRequest$BodyPublisher");
            CLS_HTTP_RESPONSE        = Class.forName("java.net.http.HttpResponse");
            CLS_BODY_HANDLERS        = Class.forName("java.net.http.HttpResponse$BodyHandlers");
            CLS_BODY_HANDLER         = Class.forName("java.net.http.HttpResponse$BodyHandler");
            CLS_HTTP_HEADERS         = Class.forName("java.net.http.HttpHeaders");
            CLS_DURATION             = Class.forName("java.time.Duration");

            M_NEW_BUILDER              = CLS_HTTP_REQUEST.getMethod("newBuilder");
            M_BUILDER_URI              = CLS_HTTP_REQUEST_BUILDER.getMethod("uri", URI.class);
            M_BUILDER_METHOD           = CLS_HTTP_REQUEST_BUILDER.getMethod(
                                             "method", String.class, CLS_BODY_PUBLISHER);
            M_BUILDER_HEADER           = CLS_HTTP_REQUEST_BUILDER.getMethod(
                                             "header", String.class, String.class);
            M_BUILDER_TIMEOUT          = CLS_HTTP_REQUEST_BUILDER.getMethod(
                                             "timeout", CLS_DURATION);
            M_BUILDER_BUILD            = CLS_HTTP_REQUEST_BUILDER.getMethod("build");
            M_PUBLISHERS_OF_BYTE_ARRAY = CLS_BODY_PUBLISHERS.getMethod(
                                             "ofByteArray", byte[].class);
            M_PUBLISHERS_NO_BODY       = CLS_BODY_PUBLISHERS.getMethod("noBody");
            M_HANDLERS_OF_BYTE_ARRAY   = CLS_BODY_HANDLERS.getMethod("ofByteArray");
            M_CLIENT_SEND              = CLS_HTTP_CLIENT.getMethod(
                                             "send", CLS_HTTP_REQUEST, CLS_BODY_HANDLER);
            M_RESPONSE_STATUS_CODE     = CLS_HTTP_RESPONSE.getMethod("statusCode");
            M_RESPONSE_BODY            = CLS_HTTP_RESPONSE.getMethod("body");
            M_RESPONSE_HEADERS         = CLS_HTTP_RESPONSE.getMethod("headers");
            M_HEADERS_MAP              = CLS_HTTP_HEADERS.getMethod("map");
            M_DURATION_OF_MILLIS       = CLS_DURATION.getMethod("ofMillis", long.class);

            /* Create the shared HttpClient via the static factory method. */
            CLIENT_INSTANCE = CLS_HTTP_CLIENT.getMethod("newHttpClient").invoke(null);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /* ------------------------------------------------------------------ *
     *  HttpExecutor implementation                                         *
     * ------------------------------------------------------------------ */

    @SuppressWarnings( { "rawtypes", "unchecked" } )
    @Override
    public HttpResponse execute(final HttpRequest request, final int timeout) throws Exception {
        final int effectiveTimeout =
            (timeout > 0) ? timeout : HttpRequest.READ_TIMEOUT;

        /* ----- Serialize request body (if present) ----- */
        byte[] bodyBytes       = null;
        String addContentType  = null;

        if (request.getInputSupplier() != null) {
            final Object object = request.getInputSupplier().get();
            if (object != null) {
                final EntityMapper.EntitySerializer serializer =
                    request.getMapper().getSerializer(object.getClass())
                        .orElseThrow(() -> new IllegalArgumentException(String.format(
                            "There is no registered serializer for type '%s'",
                            object.getClass().getCanonicalName())));
                if (request.getHeaders().getHeader("Content-Type").isEmpty()) {
                    addContentType = serializer.getContentType().toString();
                }
                bodyBytes = serializer.serialize(object);
            }
        }

        /* ----- Build BodyPublisher ----- */
        final Object bodyPublisher;
        if (bodyBytes != null) {
            bodyPublisher = M_PUBLISHERS_OF_BYTE_ARRAY.invoke(null, (Object) bodyBytes);
        } else {
            bodyPublisher = M_PUBLISHERS_NO_BODY.invoke(null);
        }

        /* ----- Build HttpRequest.Builder ----- */
        final Object builder = M_NEW_BUILDER.invoke(null);

        M_BUILDER_URI.invoke(builder, request.getUrl().toURI());
        M_BUILDER_METHOD.invoke(builder, request.getMethod().name(), bodyPublisher);

        /* Copy request headers */
        final Headers headers = request.getHeaders();
        for (final String headerName : headers.getHeaders()) {
            final List<String> values = headers.getHeaders(headerName);
            if (values.size() == 1) {
                M_BUILDER_HEADER.invoke(builder, headerName, values.get(0));
            } else if (values.size() > 1) {
                final StringBuilder sb = new StringBuilder();
                final Iterator<String> it = values.iterator();
                while (it.hasNext()) {
                    sb.append(it.next());
                    if (it.hasNext()) {
                        sb.append(',');
                    }
                }
                M_BUILDER_HEADER.invoke(builder, headerName, sb.toString());
            }
        }

        /* Add Content-Type and Content-Length headers for the serialized body */
        if (addContentType != null) {
            M_BUILDER_HEADER.invoke(builder, "Content-Type", addContentType);
        }
        if (bodyBytes != null) {
            M_BUILDER_HEADER.invoke(
                builder, "Content-Length", Integer.toString(bodyBytes.length));
        }

        /* Set request timeout */
        final Object duration =
            M_DURATION_OF_MILLIS.invoke(null, (long) effectiveTimeout);
        M_BUILDER_TIMEOUT.invoke(builder, duration);

        /* Build the java.net.http.HttpRequest */
        final Object httpRequest = M_BUILDER_BUILD.invoke(builder);

        /* ----- Send ----- */
        final Object bodyHandler   = M_HANDLERS_OF_BYTE_ARRAY.invoke(null);
        final Object httpResponse  = M_CLIENT_SEND.invoke(
            CLIENT_INSTANCE, httpRequest, bodyHandler);

        /* ----- Extract response ----- */
        final int statusCode    = (Integer) M_RESPONSE_STATUS_CODE.invoke(httpResponse);
        final byte[] rawBody    = (byte[]) M_RESPONSE_BODY.invoke(httpResponse);
        final Object httpHeaders = M_RESPONSE_HEADERS.invoke(httpResponse);

        @SuppressWarnings("unchecked")
        final Map<String, List<String>> headersMap =
            (Map<String, List<String>>) M_HEADERS_MAP.invoke(httpHeaders);

        /* ----- Build HTTP4J response ----- */
        final HttpResponse.Builder responseBuilder = HttpResponse.builder()
            .withStatus(statusCode)
            /* HTTP/2 and HTTP/1.1 via HttpClient do not expose a reason phrase. */
            .withStatusMessage("")
            .withEntityMapper(request.getMapper());

        for (final Map.Entry<String, List<String>> entry : headersMap.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            for (final String value : entry.getValue()) {
                responseBuilder.withHeader(entry.getKey(), value);
            }
        }

        if (rawBody != null && rawBody.length > 0) {
            responseBuilder.withBody(rawBody);
        }

        return responseBuilder.build();
    }

}
