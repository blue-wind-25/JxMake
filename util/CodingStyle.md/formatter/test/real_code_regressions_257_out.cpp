/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

struct Counter {

    void onAdd(size_t /*n*/) {}
    void onDrop(size_t /*n*/) {}

    enum Kind {

        SMALL, LARGE

    };  // last two are reserved
    struct Pair {

        int a;
        int b;

    };  // used by the swap helper
    int total;

}; // struct Counter

typedef int
(CALLBACK * on_event_fn)(
    int          /* code */,
    void*        /* payload */,
    unsigned int /* flags */);
