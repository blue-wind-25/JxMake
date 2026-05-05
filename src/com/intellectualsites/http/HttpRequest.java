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

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/*
 HTTP request class. This should not be interacted with directly,
 rather {@link com.intellectualsites.http.HttpClient} should be used
 */
final class HttpRequest {

    static final int READ_TIMEOUT = 3600000; // 3600 seconds

    /*
     The executor is selected once at class-load time.
     On Java 11+ we prefer java.net.http.HttpClient (via reflection); on Java 8
     we fall back to HttpURLConnection.  Any failure during the reflective
     initialisation of HttpClientExecutor is caught here and causes the fallback
     to be used instead, so the library remains fully functional on Java 8.
     */
    private static final HttpExecutor EXECUTOR;

    static {
        HttpExecutor chosen;
        try {
            Class.forName("java.net.http.HttpClient");
            chosen = new HttpClientExecutor();
        } catch (final Throwable ignored) {
            chosen = new HttpUrlConnectionExecutor();
        }
        EXECUTOR = chosen;
    }

    private final HttpMethod method;
    private final URL url;
    private final Headers headers;
    private final EntityMapper mapper;
    private final Supplier<Object> inputSupplier;
    private final Consumer<Throwable> throwableConsumer;

    private HttpRequest(final HttpMethod method, final URL url, final Headers headers,
        Supplier<Object> inputSupplier, final EntityMapper mapper,
        final Consumer<Throwable> throwableConsumer) {
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.inputSupplier = inputSupplier;
        this.mapper = mapper;
        this.throwableConsumer = throwableConsumer;
    }

    HttpMethod getMethod() {
        return this.method;
    }

    URL getUrl() {
        return this.url;
    }

    Headers getHeaders() {
        return this.headers;
    }

    EntityMapper getMapper() {
        return this.mapper;
    }

    Supplier<Object> getInputSupplier() {
        return this.inputSupplier;
    }

    /*
     Create a new request {@link Builder builder}

     @return Builder instance
     */
    static Builder newBuilder() {
        return new Builder();
    }

    HttpResponse executeRequest(int timeout) throws IOException {
        try {
            return EXECUTOR.execute(this, timeout);
        } catch (final IOException e) {
            throwableConsumer.accept(e);
        } catch (final RuntimeException e) {
            throwableConsumer.accept(e);
        } catch (final Exception e) {
            throwableConsumer.accept(e);
        } catch (final Throwable t) {
            throwableConsumer.accept(t);
        }
        return null;
    }


    static final class Builder {

        private final Headers headers = Headers.newInstance();
        private EntityMapper mapper;
        private HttpMethod method;
        private URL url;
        private Supplier<Object> inputSupplier;
        private Consumer<Throwable> throwableConsumer = Throwable::printStackTrace;

        private Builder() {
        }

        /*
         Specify the entity mapper used by the request

         @param mapper Entity mapper
         @return Builder instance
         */
        Builder withMapper(final EntityMapper mapper) {
            this.mapper = Objects.requireNonNull(mapper, "Mapper may not be null");
            return this;
        }

        /*
         Specify the HTTP method used in the request

         @param method HTTP method
         @return Builder instance
         */
        Builder withMethod(final HttpMethod method) {
            this.method = Objects.requireNonNull(method, "Method may not be null");
            return this;
        }

        /*
         Specify the URL used in the request

         @param url URL
         @return Builder instance
         */
        Builder withURL(final URL url) {
            this.url = Objects.requireNonNull(url, "URL may not be null");
            return this;
        }

        /*
         Add a header to the request

         @param key Header key
         @param value Header value
         @return Builder instance
         */
        Builder withHeader(final String key, final String value) {
            this.headers.addHeader(
                Objects.requireNonNull(key, "Key may not be null"),
                Objects.requireNonNull(value, "Value may not be null"));
            return this;
        }

        /*
         Add an input entity to the request

         @param inputSupplier Input supplier
         @return Builder instance
         */
        Builder withInput(final Supplier<Object> inputSupplier) {
            this.inputSupplier = Objects.requireNonNull(inputSupplier, "Input supplier may not be null");
            return this;
        }

        /*
         Add a throwable consumer

         @param consumer Throwable consumer
         @return Builder instance
         */
        Builder onException(final Consumer<Throwable> consumer) {
            this.throwableConsumer = Objects.requireNonNull(consumer, "Consumer may not be null");
            return this;
        }

        HttpRequest build() {
            Objects.requireNonNull(this.method, "No method was supplied");
            Objects.requireNonNull(this.url, "No URL was supplied");
            Objects.requireNonNull(this.mapper, "No mapper was supplied");
            Objects.requireNonNull(this.throwableConsumer, "No throwable consumer was supplied");
            return new HttpRequest(this.method, this.url, this.headers,
                this.inputSupplier, this.mapper, this.throwableConsumer);
        }

    }

}
