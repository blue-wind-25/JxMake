/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Found via dogfood-testing against RobotCoding gui_frontend_android's Optimizer.kt: a `when`
// expression's `else -> { ... }` branch, whose body is a multi-statement block, was flattened
// onto a single line with no `;` separators -- a Kotlin parse error, not merely an idempotency
// flap. Root cause: BlockStructureRule.collapseSingleExpressionBlocks's bare-`else` handling
// (meant for a real `if`/`else` chain's braceless single-statement body, STYLE.md §10) matches
// any KEYWORD "else" not immediately followed by "if" or "{" -- which also matches a `when`
// arm's `else` label, since its body is introduced by `->`, not a brace. Once misidentified,
// collapseBracelessBody swallowed every statement in the arm's block body onto one line with no
// separators. Fixed by also checking whether the token after `else` is `->`, and bailing out of
// the braceless-collapse path in that case (the `when`-arm formatting pass, formatWhenExpressions,
// handles `else ->` arms correctly on its own and must not have this braceless-collapse logic
// run on the same tokens first).
fun optimizerRedirect(n: Int, t: Int): Int
{
    val nt: Int = when {

        t >= n -> n
        else   -> {
            var redirected = n
            for( i in (t + 1)..n ) { if(i >= 0) { redirected = i; break } }
            redirected
        }

    } // when

    return nt
}
