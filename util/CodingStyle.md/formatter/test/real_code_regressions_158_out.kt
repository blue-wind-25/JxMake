/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

fun main()
{
    val key1 = "abc"
    val a    = $$"$key1"
    val b    = $$$"""
        raw $$key1 text
    """
}
