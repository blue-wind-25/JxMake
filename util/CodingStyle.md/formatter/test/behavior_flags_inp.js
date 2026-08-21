/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// comment with a zero-width space​ and a bidi override‮ here   
function add(a, b) {   
    const s = "non-breaking space and zero-width-joiner‍join";
    return a + b;
}