/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

package test;

public class RealCodeRegressions10 {

    void parseSuffix() {
        if (true) {
            if (last == 'u' || last == 'U') { unsigned = true; s = s.substring(0, s.length() - 1); }
        }
    }

}
