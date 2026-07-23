/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

/**
 * Example:
 *
 * ```
 * GlobalScope.launch(CoroutineExceptionHandler { _, e ->
 *     /* what to do if this coroutine fails */
 * }) {
 *     // this computation is explicitly outside structured concurrency
 * }
 * ```
 */
public fun example()
{
    println("after the doc comment")
}
