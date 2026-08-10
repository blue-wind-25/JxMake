/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
fun main() {
    val withTrailingComma = listOfExceptionallyLongNames(
        "kotlin.io.something.long.enough.to.wrap",
        "kotlin.collections.something.long.enough.to.wrap",
    )
    val withoutTrailingComma = listOfExceptionallyLongNames(
        "kotlin.io.something.long.enough.to.wrap",
        "kotlin.collections.something.long.enough.to.wrap"
    )
    val listLiteral = listOf(
        "kotlin.io.something.long.enough.to.wrap",
        "kotlin.collections.something.long.enough.to.wrap",
    )
}
