/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Found via real-code testing against `square/okio`: three co-occurring, unrelated bugs.
//
// (1) KotlinDeclarationAlignmentRule.renderTokens (used by its declaration-initializer
// Re-render path) had no notion of unary vs. binary `-`/`+` -- needsSpaceBetween is a strictly
// Pairwise (prev, cur) check with no visibility into the token before `prev`, so it
// Unconditionally inserted a space between a leading unary minus and its operand. Fixed with a
// New isUnaryMinusOperand lookback in the override.
val prefixIndex : Int = -1

// (2) ScopePipeline.applySignaturePass's Kotlin `: ReturnType` tail detection
// (findLastTopLevelCloseParen) matched a top-level `)` with no check that the span between it
// And the next open brace stays within one statement -- so a headerless multiplatform
// Declaration with no body of its own, followed by a blank line and then an unrelated later
// Declaration, had its `)`/`:` wrongly read as reaching all the way to that later declaration's
// `{`, silently merging the two into one bogus signature+tail across the blank line. Fixed with
// A new hasTopLevelBlankLine guard that bails rather than merging across a real paragraph break.
expect fun getEnv(name: String): String?

val okioRoot : OkioRoot by lazy { OkioRoot.SYSTEM.workingDirectory }

// (3) BlockStructureRule's braceless single-statement if/while/for collapse (both the
// Brace-removing tryCollapse and the already-braceless tryCollapseBraceless) rendered the
// condition prefix (e.g. "if (dataSize < requiredSize)") with the original source's
// Keyword-to-paren space preserved, one character wider than the final tightened "if(...)" form
// A later MiscRule.TIGHT_PAREN_KEYWORDS pass produces. MiscRule.enforceCallLineBreaking's
// Length check then measured the stale, pre-tightened text and over-wrapped a line that, in its
// True final width, fits exactly at the line-length limit -- an idempotency bug: reformatting
// The (wrongly) already-wrapped output then correctly measured the by-then-tightened line and
// Collapsed it back to one line. Fixed by tightening the keyword-to-paren space in the collapsed
// Prefix at collapse time, in both tryCollapse and tryCollapseBraceless.
fun readZipEntry()
{
    run {
        if(dataSize < requiredSize) throw IOException("bad zip: extended timestamp extra too short")
    }
}
