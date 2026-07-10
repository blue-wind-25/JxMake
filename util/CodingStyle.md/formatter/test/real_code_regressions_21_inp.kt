/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Found via dogfood-testing against RobotCoding gui_frontend_android's BlockCanvasView.kt: a
// `val` declaration whose initializer contains a logical-AND expression lost the space before
// the `&&` operator, rendering it flush against the preceding token. Root cause:
// DeclarationAlignmentRule.isTightToken's `Token.isRepOp(t, '*') || Token.isRepOp(t, '&')`
// check -- meant for C/C++'s repeated pointer/reference declarator sigils (`**`, `&&` as an
// rvalue-reference type) -- matches ANY token consisting solely of `&` characters, including
// Kotlin's `&&` logical-AND operator, which Kotlin has no unary/repeated `*`/`&` construct to be
// confused with. Fixed by gating both checks to non-Kotlin languages, mirroring the identical
// gate MiscRule.isTightToken already had for the same reason (STYLE_KOTLIN.md's `= expr`
// rendering, `x * x` joining as `x* x`).
class Repro {
    fun f() {
        val indentLevel = if (sel.size > 1 && pendingIndent >= 0) pendingIndent else 0
    }
}
