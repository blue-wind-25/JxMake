/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

package arrow.core

private inline fun <A> sort2(
    a: A, b: A, leq: (A, A) -> Boolean
) = if( leq(
    a, b
) ) Pair(
    a, b
) else Pair(
    b, a
)
