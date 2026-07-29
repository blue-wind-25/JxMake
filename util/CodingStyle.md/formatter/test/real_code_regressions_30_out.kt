/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Found via real-code testing against `square/okio`: three co-occurring, unrelated bugs.
//
// (1) KotlinDeclarationAlignmentRule.renderTokens (used by its declaration-initializer
// re-render path) had no notion of unary vs. binary `-`/`+` -- needsSpaceBetween is a strictly
// pairwise (prev, cur) check with no visibility into the token before `prev`, so it
// unconditionally inserted a space between a leading unary minus and its operand. Fixed with a
// new isUnaryMinusOperand lookback in the override.
val prefixIndex : Int = -1

// (2) ScopePipeline.applySignaturePass's Kotlin `: ReturnType` tail detection
// (findLastTopLevelCloseParen) matched a top-level `)` with no check that the span between it
// and the next open brace stays within one statement -- so a headerless multiplatform
// declaration with no body of its own, followed by a blank line and then an unrelated later
// declaration, had its `)`/`:` wrongly read as reaching all the way to that later declaration's
// `{`, silently merging the two into one bogus signature+tail across the blank line. Fixed with
// a new hasTopLevelBlankLine guard that bails rather than merging across a real paragraph break.
expect fun getEnv(name: String): String?

val okioRoot: OkioRoot by lazy { OkioRoot.SYSTEM.workingDirectory }

// (3) BlockStructureRule's braceless single-statement if/while/for collapse (both the
// brace-removing tryCollapse and the already-braceless tryCollapseBraceless) rendered the
// condition prefix (e.g. "if (dataSize < requiredSize)") with the original source's
// keyword-to-paren space preserved, one character wider than the final tightened "if(...)" form
// a later MiscRule.TIGHT_PAREN_KEYWORDS pass produces. MiscRule.enforceCallLineBreaking's
// length check then measured the stale, pre-tightened text and over-wrapped a line that, in its
// true final width, fits exactly at the line-length limit -- an idempotency bug: reformatting
// the (wrongly) already-wrapped output then correctly measured the by-then-tightened line and
// collapsed it back to one line. Fixed by tightening the keyword-to-paren space in the collapsed
// prefix at collapse time, in both tryCollapse and tryCollapseBraceless.
fun readZipEntry()
{
    run {
        if(dataSize < requiredSize) throw IOException("bad zip: extended timestamp extra too short")
    }
}
