/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

namespace Outer {
    namespace Detail {
        void f() {
            id x = [[NSString alloc] init];
        }
    }

    struct S {
        void run() {
            try {
                doWork();
            }
            catch (std::exception const& ex) {
                report(ex);
            }
        }
    };

    void g() {
        if (cond) {
            doA();
        } else if (other) {
            doB();
        } else {
            doC();
        }
    }
}
