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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*
 {@link HttpExecutor} implementation that uses {@link java.net.HttpURLConnection}.
 This is the Java-8-compatible transport path and is also used as the fallback on
 newer runtimes when the {@code java.net.http.HttpClient} reflective set-up fails.
 */
final class HttpUrlConnectionExecutor implements HttpExecutor {

    @SuppressWarnings( { "rawtypes", "unchecked" } )
    @Override
    public HttpResponse execute(final HttpRequest request, final int timeout) throws IOException {
        final HttpURLConnection connection =
            (HttpURLConnection) request.getUrl().openConnection();
        try {
            final int effectiveTimeout =
                (timeout > 0) ? timeout : HttpRequest.READ_TIMEOUT;

            connection.setRequestMethod(request.getMethod().name());
            connection.setDoOutput(request.getMethod().hasBody());
            connection.setUseCaches(false);
            connection.setConnectTimeout(effectiveTimeout);
            connection.setReadTimeout(effectiveTimeout);

            connection.setRequestProperty("User-Agent", jxm.SysUtil._JxMakeUserAgent);

            final Headers headers = request.getHeaders();
            for (final String headerName : headers.getHeaders()) {
                final List<String> values = headers.getHeaders(headerName);
                if (values.size() == 1) {
                    connection.addRequestProperty(headerName, values.get(0));
                } else if (values.size() > 1) {
                    final StringBuilder sb = new StringBuilder();
                    final Iterator<String> it = values.iterator();
                    while (it.hasNext()) {
                        sb.append(it.next());
                        if (it.hasNext()) {
                            sb.append(',');
                        }
                    }
                    connection.addRequestProperty(headerName, sb.toString());
                }
            }

            connection.setDoInput(true);
            connection.setDoOutput(request.getInputSupplier() != null);

            if (request.getInputSupplier() != null) {
                final Object object = request.getInputSupplier().get();
                if (object != null) {
                    final EntityMapper.EntitySerializer serializer =
                        request.getMapper().getSerializer(object.getClass())
                            .orElseThrow(() -> new IllegalArgumentException(String.format(
                                "There is no registered serializer for type '%s'",
                                object.getClass().getCanonicalName())));
                    if (headers.getHeader("Content-Type").isEmpty()) {
                        connection.setRequestProperty(
                            "Content-Type", serializer.getContentType().toString());
                    }
                    final byte[] bytes = serializer.serialize(object);
                    connection.setRequestProperty(
                        "Content-Length", Integer.toString(bytes.length));
                    try (final DataOutputStream dos =
                             new DataOutputStream(connection.getOutputStream())) {
                        dos.write(bytes);
                        dos.flush();
                    }
                }
            }

            connection.connect();

            final InputStream stream;
            if (request.getMethod().hasBody()) {
                if (connection.getResponseCode() >= 400) {
                    stream = connection.getErrorStream();
                } else {
                    stream = connection.getInputStream();
                }
            } else {
                stream = null;
            }

            final HttpResponse.Builder builder = HttpResponse.builder()
                .withStatus(connection.getResponseCode())
                .withStatusMessage(connection.getResponseMessage())
                .withEntityMapper(request.getMapper());

            for (final Map.Entry<String, List<String>> entry :
                connection.getHeaderFields().entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                for (final String value : entry.getValue()) {
                    builder.withHeader(entry.getKey(), value);
                }
            }

            if (stream != null) {
                try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    int b;
                    while ((b = stream.read()) != -1) {
                        bos.write(b);
                    }
                    builder.withBody(bos.toByteArray());
                }
            }

            return builder.build();
        } finally {
            connection.disconnect();
        }
    }

}
