/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Found via dogfood-testing against RobotCoding gui_frontend_android's ToolbarActions.kt /
// MainViewModel.kt (surfaced once RDD_KEY_136 stopped masking it): a val whose initializer is a
// parenthesized if/else expression (`(if (cond) a else b)`), immediately followed by another
// statement in the same scope, was fused onto that following statement's line with no separator
// at all -- invalid Kotlin. BlockStructureRule.collapseSingleExpressionBlocks has no notion of
// expression- vs statement-position `if`/`else`; it fired on the expression-position `if` here
// (which has no braced body) and treated everything up to and past the wrapping `)` as if it were
// a braceless statement body, swallowing the newline before the next statement.
class Repro {
    private fun showResult(context: Context, warning: String?)
    {
        val display = (if (warning != null) "$warning\n\n" else "") + "Done"
        showMessage(context, display)
    }
}
