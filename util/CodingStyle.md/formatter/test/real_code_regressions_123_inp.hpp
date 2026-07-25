/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class IdStrategy {
    void keyFor() {
        int index = id.lastIndexOf('@'); // The @ can be used in local-part if quoted correctly
        // => the last @ is the one used to separate the domain and local-part
    }

    int a() { return 1; } // Count : 1
    int bb() { return 2; } // GrandTotal : 22
};
