/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Found via dogfood-testing against RobotCoding gui_frontend_android's BlockPalette.kt: a run of
// Adjacent §9 expression-bodied one-liner functions, one of which (`_addPlayMusic`) has a body
// long enough that a later phase (MiscRule.enforceCallLineBreaking) wraps its call across
// Multiple lines once column-padding is added. On a fresh format the run's column width was
// Computed from every member's original (still short, still single-line) text, including
// `_addPlayMusic`'s -- only for that later wrapping phase to break it across lines afterward,
// Leaving the group's padding decision stale. Reformatting that already-wrapped output then
// Correctly excluded the now-multi-line `_addPlayMusic` via
// KotlinGetterSetterRule.parseKotlinOneLinerMember's `hasNewlineBetween` bail, splitting the run
// Into different subgroups with different (narrower) column widths on the second pass -- an
// Idempotency flap. Root cause: this exact class of bug was already fixed for the C/C++/Java
// Base class (GetterSetterRule.parseOneLinerMember's own length pre-check, see its doc comment)
// But never ported to this Kotlin sibling method. Fixed by adding the same
// `hasBreakableCall` + estimated-width pre-check, excluding a too-long member from the group on
// The very first pass too, so the decision (and thus the padding) is stable across repeated
// Formats.
class Repro {

    private fun _addMove()  = _canvas.startHolding( MoveBlock(MoveBlock.Dir.FORWARD, "1") )
    private fun _addTurn()  = _canvas.startHolding( TurnBlock(TurnBlock.Dir.LEFT, "1") )
    private fun _addWait()  = _canvas.startHolding( WaitBlock("1000") )
    private fun _addPlayMusic()  = _canvas.startHolding(
        PlayMusicBlock("{EGc}4 {EGc}>4 {EGc}4 {EGc}<4")
    )
    private fun _addSetVar()     = _canvas.startHolding( SetVariableBlock("X", "0") )

} // class Repro
