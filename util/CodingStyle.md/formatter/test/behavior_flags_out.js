/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Comment with a zero-width space<U+200B> and a bidi override<U+202E> here
function add(a, b)
{
    const s = "non-breaking\u00a0space and zero-width-joiner\u200djoin";

    return a + b;
}
