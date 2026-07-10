/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Found via dogfood-testing against RobotCoding gui_frontend_android's ConnectTypeDialog.kt and
// WifiApDialog.kt (`.show().also { ... }`): ScopePipeline.applySignaturePass's Kotlin `: ReturnType`
// Tail handling (STYLE_KOTLIN.md §9) uses findLastTopLevelCloseParen to locate the parameter list's
// Own closing paren when it isn't the token immediately before the body's `{` -- but that helper
// Only scanned for the LAST depth-0 `)` in range, with no check that a genuine `:` actually follows
// it before the brace. A fluent chain of the shape `x.foo().bar { ... }` -- where the FIRST call has
// Empty parens and the SECOND call uses Kotlin's bare/parenless trailing-lambda call syntax -- has
// Exactly this token shape (`)` ... IDENTIFIER `{`) with no `:` at all, so `foo()`'s `)` was wrongly
// Accepted as if it were a signature's parameter list and `bar` as if it were a return-type tail,
// Causing `.bar` to be silently deleted from the rendered output on the very first format pass (not
// Merely an idempotency flap -- first-pass wrong/uncompilable output). Fixed by requiring a top-level
// `:` immediately after the found `)` (via nextSignificantIndex) before accepting the Kotlin
// return-type-tail branch in applySignaturePass; otherwise bail (continue) rather than misdetect.
class Repro {

    fun build()
    {
        x.foo().bar { it.f() }
    }

} // class Repro
