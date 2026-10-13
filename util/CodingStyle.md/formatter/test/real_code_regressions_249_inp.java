/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

class ArithSpacing {
    private static final long DURATION_MS = 20L * 60L * 1000L;
    private static final int TIMEOUT = 20 * 60 * 1000;

    int f(int a, int[] v) {
        int y = v[0] * 2 * (a + 1);
        int z = (a + 1) * a;
        return y * z;
    }
}
