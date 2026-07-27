/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

object Runtime {
    fun makeLambda() {
        val lambda = @JsNoLifting { throwUnsupportedOperationException("no lifting") }
    }
}
