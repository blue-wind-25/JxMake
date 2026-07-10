/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Found via dogfood-testing against RobotCoding gui_frontend_android's PlayMusicBlock.kt: an
// Idempotency bug in KotlinDeclarationAlignmentRule.spansMultipleLines. On a fresh format, a
// Braceless `if(cond) expr else expr` initializer that's short enough to stay on one line groups
// And column-aligns normally with an adjacent `val` sibling. But if that initializer's own nested
// Call gets wrapped across lines by MiscRule.enforceCallLineBreaking (because the whole statement
// Is too long), a *second* format pass sees those wrapping newlines and wrongly treated the
// Initializer as a genuine multi-line block expression, bailing it out of its alignment group --
// Shrinking the sibling `val`'s own column padding on every successive pass.
class Repro {

    fun draw(x: Int)
    {
        val full    = "MUSIC A $x plus a very long padding constant to force line wrapping"
        val display = if(full.length > MAX_LABEL_CHARS) full.substring(
            0, MAX_LABEL_CHARS - 1
        ) + "..."
        else full
    }

} // class Repro
