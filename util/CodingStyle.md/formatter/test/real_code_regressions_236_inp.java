/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

/*% JXM_CFMT_CFG line-split-operator-priority=on */

public final class RealCodeRegressions236 {
    public static void foo() {
        while (true) {
            int a = 1;
            int b = 2;
            int c = 3;
            if (someVeryLongConditionNameAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA && anotherVeryLongConditionNameBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB) {
                doSomething();
            }
        }
    }
}
