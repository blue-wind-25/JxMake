/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Found via dogfood-testing against RobotCoding gui_frontend_android's BlockCanvasView.kt
// (`class BlockCanvasView @JvmOverloads constructor(`) and ToolbarActions.kt (a second adjacent
// `@Volatile private var` declaration): KotlinSpecificRule.enforceLabeledJumpSpacing's small state
// machine, meant to detect Kotlin's `label@` loop-label declaration syntax (STYLE_KOTLIN.md §11,
// e.g. `outer@ for (...) { ... }`) and tighten the identifier/`@` gap, had no way to tell a genuine
// label apart from an unrelated `@Annotation` that merely happens to sit right after some other
// identifier -- a class name (`class BlockCanvasView @JvmOverloads constructor(...)`) or an enum
// constant ending the previous statement (`= OptimizerLevel.NONE` immediately before the next
// line's own `@Volatile ...` declaration). Both shapes were misread as `IDENTIFIER@`, tightening
// (or, across a blocked newline gap, at least state-transitioning as if tightening) the identifier
// against `@` and then force-inserting a space between `@` and the following identifier -- silently
// corrupting `@JvmOverloads`/`@Volatile` into `@ JvmOverloads`/`@ Volatile`, a parse error
// ("Expected annotation identifier after '@'" for the space-before-name shape) on the very first
// format pass, not merely an idempotency flap. Fixed by adding a lookahead, `isLoopLabelTarget`,
// requiring the token after `@` to actually be `for`/`while`/`do` or `{` (a labeled lambda literal,
// e.g. `"123".let parse@ { ... }`) -- the only constructs a Kotlin label can legally prefix -- before
// treating an `IDENTIFIER @` sequence as a label declaration at all, both for the state-machine
// transition into `AFTER_DECL_AT` and for the `tightBeforeAt` spacing decision itself (both needed
// the same guard; fixing only the state transition left the tight-adjacency case, `class Foo
// @JvmOverloads constructor(...)`, still broken since that decision is made independently).
class Repro @JvmOverloads constructor(
    context: Int
) {
    @Volatile private var _lastCompileLevel = OptimizerLevel.NONE
    @Volatile private var _lastSendLevel = OptimizerLevel.NONE

    fun labeled() {
        outer@ for (i in 1..3) {
            for (j in 1..3) {
                if (j == 2) break@outer
            }
        }
        "x".let parse@ {
            if (it.isEmpty()) return@parse 0
            1
        }
    }
}
