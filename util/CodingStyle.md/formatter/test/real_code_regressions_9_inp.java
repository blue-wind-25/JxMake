/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

package test;

public class RealCodeRegressions9 {

    Value eval(int type) {
        switch (type) {
            case CPP_PLUS: return add(b);
            case CPP_MINUS_LONG_TOKEN_NAME_TO_WIDEN_COLUMN: return sub(b);
            default: return unrecognizedOperator(type, alpha, beta, gamma);
        }
    }

}
