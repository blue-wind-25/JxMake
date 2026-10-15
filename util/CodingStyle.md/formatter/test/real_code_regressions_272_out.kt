/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

fun check(list: List<Int>, flag: Boolean): Boolean
{
    if(flag && list.any { y ->
            y > 10
        }) {
        return true
    }
    while( list.isNotEmpty() && list.all {
            it > 0
        } ) {
        return false
    }

    return false
}
