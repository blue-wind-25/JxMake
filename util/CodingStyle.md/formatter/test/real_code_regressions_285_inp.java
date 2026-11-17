/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

class Casts {
    Object f(Object o, URL url, int x, Object[] arr) throws Exception {
        final HttpURLConnection a1 = (HttpURLConnection)url.openConnection();
        final HttpURLConnection a2 = (HttpURLConnection) url.openConnection();
        final java.net.HttpURLConnection q = (java.net.HttpURLConnection)url.openConnection();
        a1 = (HttpURLConnection)url.openConnection();
        foo((String)o, (int)-x, (long)(x * 2), (java.util.List<String>)o);
        if (o != null) bar((Object)this, (char)'a', (boolean)!flag);
        int y = (x) - 1;
        Object p = (a) -> b;
        synchronized (lock) { x++; }
        g((String[])arr);
        return (String)o;
    }
}
