/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

fun sample(expected: Array<*>?, actual: Array<*>?) {
    assertArrayContentEquals(expected, actual, Array<*>?::contentToString, Array<*>?::contentEquals)
}
