/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Found via dogfood-testing against RobotCoding gui_frontend_android's Optimizer.kt and
// ProgramBuilder.kt -- two separate, unrelated bugs both surfaced by the same `when`-arm/
// Declaration-initializer shapes, folded into this one fixture since they sit in the same
// Source line shape and were both root-caused in the same session:
//
// (1) `it !is _FunctionItem` (RDD_KEY_143): DeclarationAlignmentRule.needsSpaceBetween had no
//     Notion of Kotlin's `!is`/`!in` negated type-check/containment operators as a single tight
//     Lexical unit -- the generic KEYWORD-gets-a-leading-space default inserted a space between
//     `!` and `is`, corrupting it into `! is`, a parse error. Fixed by special-casing `!` +
//     `is`/`in` to render with no space.
//
// (2) `calledFunctions.contains(it.func.funcName)` (RDD_KEY_144): once the enclosing
//     Declaration's line grew too long to fit, MiscRule.enforceCallLineBreaking wrapped this
//     Call's own single argument onto its own line -- but first ran it through
//     MiscRule.parseSignature (meant to detect a real C/C++/Java-style forward declaration's
//     Typed parameter list) to decide which render path to use. parseParam's generic "last
//     IDENTIFIER is the name, everything before it is the type" heuristic misparsed the dotted
//     Member-access expression `it.func.funcName` (no top-level comma, single IDENTIFIER-ending
//     Token run) as if it were a `Type name` pair (type `it.func.`, name `funcName`) -- Kotlin
//     Has no such prototype-only declaration shape at all (`fun` always introduces one, with the
//     Reversed `name: Type` order), so this C-style heuristic should never apply to a Kotlin
//     Candidate. The typed dropped-form render then inserted a column-separator space between
//     The misdetected "type" and "name" cells, corrupting the expression into
//     `it.func. funcName`. Fixed by forcing every Kotlin candidate's render-path selection
//     Through the untyped call-argument path (which only ever reproduces existing whitespace,
//     Never inserts a new one) while still keeping the zero-param bail-out (needed so a Kotlin
//     Function's own empty `()` parameter list is never wrapped either).
class Foo {

    fun toCommandArray(): Int
    {
        val liveItems = _items.filter { it !is _FunctionItem || calledFunctions.contains(
            it.func.funcName
        ) }

        return 0
    }

    private fun _addPlayMusic() = _canvas.startHolding()

} // class Foo
