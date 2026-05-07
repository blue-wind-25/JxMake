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

import java.util.Objects;

/*
 A HTTP response
 */
public final class HttpResponse {

    private final Headers headers;
    private final EntityMapper entityMapper;
    private final int code;
    private final String status;
    private final byte[] body;

    private HttpResponse(final int code,
                         final String status,
                         final Headers headers,
                         final EntityMapper entityMapper,
                         final byte[] body) {
        this.status = status;
        this.code = code;
        this.headers = headers;
        this.entityMapper = entityMapper;
        this.body = body;
    }

    /*
     Create a new builder instance

     @return Builder instance
     */
    static Builder builder() {
        return new Builder();
    }

    /*
     Get the HTTP status message

     @return Status message
     */
    public String getStatus() {
        return this.status;
    }

    /*
     Get a standard reason phrase for a given HTTP status code.
     Compensates for java.net.http.HttpClient not providing this information.
     */
    public static String getReasonPhrase(final int code) {
        switch (code) {
            case 100: return "Continue";
            case 101: return "Switching Protocols";
            case 200: return "OK";
            case 201: return "Created";
            case 202: return "Accepted";
            case 203: return "Non-Authoritative Information";
            case 204: return "No Content";
            case 205: return "Reset Content";
            case 206: return "Partial Content";
            case 300: return "Multiple Choices";
            case 301: return "Moved Permanently";
            case 302: return "Found";
            case 303: return "See Other";
            case 304: return "Not Modified";
            case 305: return "Use Proxy";
            case 307: return "Temporary Redirect";
            case 308: return "Permanent Redirect";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 402: return "Payment Required";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 406: return "Not Acceptable";
            case 407: return "Proxy Authentication Required";
            case 408: return "Request Timeout";
            case 409: return "Conflict";
            case 410: return "Gone";
            case 411: return "Length Required";
            case 412: return "Precondition Failed";
            case 413: return "Payload Too Large";
            case 414: return "URI Too Long";
            case 415: return "Unsupported Media Type";
            case 416: return "Range Not Satisfiable";
            case 417: return "Expectation Failed";
            case 426: return "Upgrade Required";
            case 428: return "Precondition Required";
            case 429: return "Too Many Requests";
            case 431: return "Request Header Fields Too Large";
            case 451: return "Unavailable For Legal Reasons";
            case 500: return "Internal Server Error";
            case 501: return "Not Implemented";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            case 504: return "Gateway Timeout";
            case 505: return "HTTP Version Not Supported";
            case 511: return "Network Authentication Required";
            default:  return "";
        }
    }

    /*
     Get the HTTP status code

     @return Status code
     */
    public int getStatusCode() {
        return this.code;
    }

    /*
     Get the raw response body

     @return Response body
     */
    public byte[] getRawResponse() {
        return this.body;
    }

    /*
     Get the response headers

     @return Response headers
     */
    public Headers getHeaders() {
        return this.headers;
    }

    /*
     Get the response entity and map it to a specific type

     @param returnType Return type class
     @param <T> Return type
     @return Response
     @throws IllegalArgumentException If no mapper exists for the type
     */
    public <T> T getResponseEntity(final Class<T> returnType) {
        final String contentTypeString = this.headers.getOrDefault("content-type", null);
        final ContentType contentType;
        if (contentTypeString != null) {
            contentType = ContentType.of(contentTypeString);
        } else {
            contentType = null;
        }

        return this.entityMapper.getDeserializer(returnType).map(deserializer ->
            deserializer.deserialize(contentType, this.getRawResponse()))
            .orElseThrow(() -> new IllegalStateException(String.format("Could not deserialize response into type '%s'",
                returnType.getCanonicalName())));
    }


    static class Builder {

        private final Headers headers = Headers.newInstance();
        private int status;
        private String statusMessage;
        private EntityMapper entityMapper;
        private byte[] bytes = new byte[0];

        private Builder() {
        }

        Builder withStatus(final int status) {
            this.status = status;
            return this;
        }

        Builder withStatusMessage(final String statusMessage) {
            this.statusMessage = statusMessage;
            return this;
        }

        Builder withHeader(final String key, final String value) {
            this.headers.addHeader(Objects.requireNonNull(key, "Key may not be null"),
                Objects.requireNonNull(value, "Value may not be null"));
            return this;
        }

        Builder withEntityMapper(final EntityMapper entityMapper) {
            this.entityMapper = Objects.requireNonNull(entityMapper, "Mapper may not be null");
            return this;
        }

        Builder withBody(final byte[] bytes) {
            this.bytes = Objects.requireNonNull(bytes, "Bytes may not be null");
            return this;
        }

        HttpResponse build() {
            return new HttpResponse(this.status, this.statusMessage,
                this.headers, this.entityMapper, this.bytes);
        }

    }

}
