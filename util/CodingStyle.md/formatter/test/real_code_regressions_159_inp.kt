/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

fun computeAllSupertypes(result: MutableList<Int>) {
    if (result.all {
            val klass = it
            klass != null && (klass == 1 || klass == 2)
        }) {
        result += 3
    }
}

fun checkOther(list: List<Int>, flag: Boolean): Boolean {
    if (flag && list.any {
            val y = it + 1
            y > 10
        }) {
        return true
    }
    return false
}
