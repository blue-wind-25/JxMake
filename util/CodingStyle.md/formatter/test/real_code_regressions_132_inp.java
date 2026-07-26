/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

class M {
    void f() {
        if (a == Kind.LIST) {
            if (!el.isEmpty()) {
                x += 1;
            }
        } else if ((a == Kind.SET || a == Kind.TUPLE) && !el.isEmpty()) {
                x += 2;
            }
    }
}
