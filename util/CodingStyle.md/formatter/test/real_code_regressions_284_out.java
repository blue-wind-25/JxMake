/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

class Qualified {

    java.util.Map.Entry<String, Integer> entry;
    int                                  count = 0;

    void shutdown(int port, Obj obj) throws java.io.IOException
    {
        final java.net.URL url = java.net.URI.create("http://localhost:" + port).toURL();
        final java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
        obj.field  = 2;
        this.value = 3;
        java.util.concurrent.atomic.AtomicBoolean done = new java.util.concurrent.atomic.AtomicBoolean();
        int n = 1;
    }

    void ping(int port) throws java.io.IOException
    {
        final java.net.URL               u = java.net.URI.create("http://h:" + port).toURL();
        final java.net.HttpURLConnection c = (java.net.HttpURLConnection) u.openConnection();
    }

    void lambdas(Model ex, java.util.Map<String, int[]> shares)
    {
        java.util.function.UnaryOperator<float[]> id = x -> x;
        int[]                                     s  = shares.computeIfAbsent(
            "a", k -> new int[5]
        );
    }

} // class Qualified
