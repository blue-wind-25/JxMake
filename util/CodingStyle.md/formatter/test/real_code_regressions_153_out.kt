/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

val onClick : @Composable (Int) -> Unit = {}

fun Foo(content: @Composable () -> Unit)
{
}

val lam = @JsNoLifting { println("hi") }
