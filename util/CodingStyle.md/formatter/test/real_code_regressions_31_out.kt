/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Found via `kotlinc` compile-checking against `square/okio` (real-code testing turned up two
// More compile-breaking bugs that plain round1-vs-round2 idempotency diffing missed, since both
// Are broken consistently -- i.e. "stably" -- from the very first format pass).
//
// (1) TokenizerCore.MULTI_CHAR_OPS was entirely missing Kotlin's `===`/`!==` referential
// Equality/inequality operators -- only their 2-char value-equality prefixes `==`/`!=` were
// Present. `next !== this` lexed as the two separate tokens `!=` and `=`, which a later
// Paren-tightening pass then re-spaced into the invalid `!= =`. Fixed by adding `===`/`!==`
// Ahead of `==`/`!=` in the longest-prefix-first list.
class Segment {

    fun pop(): Segment?
    {
        val result = if(next !== this) next else null

        return result
    }

} // class Segment

// (2) BlockStructureRule's braceless-collapse main dispatch treats every KEYWORD "while" as a
// Candidate loop-start needing a `(cond)` plus optional braceless body, with no check that this
// "while" is actually the trailing keyword of a preceding `do { ... } while (cond)` -- which has
// Nothing after its own `)` but a statement terminator, the same shape as an already-braceless
// Multi-line body. The following, otherwise-unrelated statement got misread as that "while"'s
// Own body and fused onto the same line with no separator. Fixed with a new
// IsDoWhileTailKeyword lookback (previous significant token is a `}` whose matching `{` is
// Itself immediately preceded by a `do` keyword) that bails the collapse dispatch for that shape.
fun readAll()
{
    var seen = 0
    var done = false
    do {
        seen += 1
    } while(!done && head != null)

    size -= seen.toLong()
}
