/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

public class T {

    void m(boolean cond)
    {
        if(cond) {
            Supplier<String> supplier = compute();
        }

        while(cond) {
            Foo x = new Foo();
        }

        for(int i = 0; i < 1; ++i) {
            List<Map<String, Integer>> data = build();
        }
    }

} // class T
