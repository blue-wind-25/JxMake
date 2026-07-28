/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
fun check(port: Int?, x: String?)
{
    assert(port!! in COMPILE_DAEMON_PORTS_RANGE_START..<COMPILE_DAEMON_PORTS_RANGE_END)
    val y = x!!.length
    val z = x!! ?: "default"
}
