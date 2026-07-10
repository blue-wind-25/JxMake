/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Found via dogfood-testing against RobotCoding gui_frontend_android's BlockPalette.kt: an
// `override fun draw(...)` method body inside an anonymous `object : Block() { ... }` whose single
// call (`_drawBlock(...)`) is short enough that the whole `{ ... }` body still fits on one physical
// line pre-formatting, but is long enough (at this method's real indentation depth) that a later
// phase (MiscRule.enforceCallLineBreaking) wraps the call across multiple lines anyway. On a fresh
// format, KotlinSpecificRule.isSingleLineBody saw the body still on one physical line (pre-wrap) and
// kept `{` K&R inline; only the later call-wrapping phase split it internally, leaving the K&R brace
// stale. Reformatting that already-wrapped output then correctly saw a genuinely multi-line body and
// moved `{` to Allman -- an idempotency flap identical in shape to the one already fixed on
// JavaSpecificRule.isSingleLineBody (never previously ported to this Kotlin sibling method) and to
// KotlinGetterSetterRule.parseKotlinOneLinerMember (RDD_KEY_139). Fixed by adding the same
// `hasBreakableCall` + estimated-width pre-check to KotlinSpecificRule.isSingleLineBody, this time
// also correcting the estimated-width formula (ported from JavaSpecificRule) to include indentation
// and inter-token spacing, which the original formula omitted -- a shortfall too small to matter at
// JavaSpecificRule's shallower nesting depths, but large enough at this Kotlin method's deeper
// (anonymous-object-nested) indentation to produce a false "fits" verdict without the fix.
class Repro {
    private fun _makeRectPreview(label: String, fill: Int, stroke: Int): Block =
        object : Block() {
            override fun draw(canvas: Canvas, x: Int, y: Int) { _drawBlock(canvas, x, y, fill, stroke, label) }
            override fun execute(): Array<Any> = emptyArray()
        }
}
