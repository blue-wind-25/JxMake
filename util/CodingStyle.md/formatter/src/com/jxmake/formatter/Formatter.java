/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter;

import com.jxmake.formatter.rules.BlockStructureRule;
import com.jxmake.formatter.rules.CppSpecificRule;
import com.jxmake.formatter.rules.JavaSpecificRule;
import com.jxmake.formatter.rules.MiscRule;
import com.jxmake.formatter.rules.SwitchRule;
import com.jxmake.formatter.tokenizer.TokenizerCore;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;

import java.util.List;

public final class Formatter {
    private Formatter() {
    }

    public static String formatOne(final String content, final String language, final String filePath,
            final Config config) {
        return formatOne(content, language, filePath, config, false);
    }

    public static String formatOne(final String content, final String language, final String filePath,
            final Config config, final boolean formatOff) {
        final Lang lang = new Lang(language);
        final TokenizerCore tokenizerCore = new TokenizerCore(lang);
        final boolean isCpp = lang.isCpp;
        final boolean isC = lang.isC;
        final boolean isCOrCpp = isCpp || isC;
        final boolean isJava = lang.isJava;

        final java.util.function.Function<String, List<Token>> tokenizer = (final String s) -> {
            final List<Token> tokens = tokenizerCore.tokenize(s);
            TokenizerCore.markFrozenSpans(tokens, formatOff);
            return tokens;
        };

        final BlockStructureRule blockRule = new BlockStructureRule(lang, config.closingCommentMinLines());
        final SwitchRule switchRule = new SwitchRule(lang);
        final MiscRule miscRule = new MiscRule(lang, config.isNormalizeCommentStartCase(),
                config.isNormalizeCommentEndPeriod());
        final CppSpecificRule cppRule = isCOrCpp ? new CppSpecificRule(lang) : null;
        final JavaSpecificRule javaRule = isJava ? new JavaSpecificRule(lang) : null;

        // Phase 0: §5/§6/§8/§14 grouping rules, recursive.
        // Pre-pad complexity spacing (§3.1) before grouping/column-width computation -- otherwise
        // GetterSetterRule's body-column width can be measured against a body's pre-padding text
        // (e.g. `Math.max( 0, lvl )`) which Phase 1's own enforceComplexityPadding call later
        // shrinks (e.g. to `Math.max(0, lvl)`), leaving a sibling member's trailing padding stale
        // by the amount stripped -- stable only on a second format pass, when the input is already
        // post-padding. enforceComplexityPadding is idempotent, so re-running it in Phase 1 after
        // Phase 0's own transformations (which can introduce new one-liner bodies) is still needed
        // and safe.
        String text = miscRule.enforceComplexityPadding(tokenizer.apply(content));
        text = new ScopePipeline(lang, config.indentStyle(), config.isNormalizeCommentStartCase(),
                config.isNormalizeCommentEndPeriod(), formatOff).process(text);

        // Phase 1: structural/brace passes.
        text = blockRule.collapseSingleExpressionBlocks(tokenizer.apply(text));
        text = blockRule.enforceKAndRBraceStyle(tokenizer.apply(text));
        text = blockRule.enforceNamedConstructHeaderSpacing(tokenizer.apply(text));
        text = blockRule.placeElseOnOwnLine(tokenizer.apply(text));
        text = blockRule.placeCatchFinallyOnOwnLine(tokenizer.apply(text));
        text = blockRule.insertNamedConstructBlankLines(tokenizer.apply(text));
        if (isCOrCpp) {
            text = cppRule.enforceFunctionDefinitionAllmanBraceStyle(tokenizer.apply(text));
            text = cppRule.enforceEmptyParameterList(tokenizer.apply(text));
            if (isCpp) {
                text = cppRule.enforceRequiresClausePlacement(tokenizer.apply(text));
            }
        } else if (isJava) {
            text = javaRule.enforceMethodDefinitionAllmanBraceStyle(tokenizer.apply(text));
            text = javaRule.enforcePermitsClauseLineBreaking(tokenizer.apply(text));
            text = javaRule.separateEnumConstantListTerminator(tokenizer.apply(text));
        }
        // enforceCallLineBreaking's "does it fit in LINE_LENGTH_LIMIT" measurement must see
        // enforceComplexityPadding's loose `( x )` spacing already applied -- otherwise a line
        // right at the boundary can measure as "fits" here, then grow past the limit once padding
        // is added with no further re-check, only to (correctly, but inconsistently) get broken
        // the next time the file is formatted. So enforceComplexityPadding is pulled forward from
        // Phase 4 to run right before it, rather than moving enforceCallLineBreaking itself back
        // -- it must stay ahead of Phase 3's addClosingComments (same reasoning as the
        // alignInlineSwitches-before-addClosingComments comment below: a pass that can expand a
        // block's line count has to run before any line-count-threshold decision reads it, or that
        // decision sees a pre-expansion count on a fresh format and a post-expansion count on a
        // reformat of already-formatted output). Found via real-world testing (tinyexpr-plusplus's
        // bitwise_rotate_right/left overloads and te_parser::list's while loop,
        // github.com/blake-madden/tinyexpr-plusplus).
        text = miscRule.enforceComplexityPadding(tokenizer.apply(text));
        text = miscRule.enforceCallLineBreaking(tokenizer.apply(text));
        // enforceCallLineBreaking can join a call whose args originally spanned multiple lines
        // (each side's own gap blocked the pass above from touching its spacing, since a NEWLINE
        // in the gap suppresses the rewrite for that side) onto a single line, replacing the
        // newline with a plain single space with no complexity-padding awareness of its own. On a
        // fresh format that plain-space join is exactly what "loose" padding looks like, even for
        // an argument list with no nested `(`/`[` that should render tight -- stable only on a
        // second pass, once there's no longer a NEWLINE in the gap to block the rewrite. Re-running
        // enforceComplexityPadding here (idempotent, purely paren-local) re-tightens/loosens any
        // call whose layout enforceCallLineBreaking just finalized.
        text = miscRule.enforceComplexityPadding(tokenizer.apply(text));
        text = switchRule.formatNonInlineSwitches(tokenizer.apply(text));
        text = miscRule.insertBlankLineBeforeReturn(tokenizer.apply(text));

        // Phase 2: comment-style normalization -- must precede Phase 3 (RDD_KEY_47).
        text = miscRule.enforceCommentStyle(tokenizer.apply(text));
        text = miscRule.alignCommentSeparators(tokenizer.apply(text));

        // Phase 3: comment/marker-generating passes.
        // `alignInlineSwitches`/`markFallthrough` run before `addClosingComments` so the SWITCH
        // closing-comment line-count decision (STYLE.md §7) sees the switch body's final,
        // fully-compacted line count -- not its pre-alignment shape -- keeping the decision
        // (and thus idempotency) stable across repeated format passes.
        text = switchRule.markFallthrough(tokenizer.apply(text));
        text = switchRule.alignInlineSwitches(tokenizer.apply(text));
        text = blockRule.addClosingComments(tokenizer.apply(text));
        if (isJava) {
            text = javaRule.enforceSwitchExpressionArrowAlignment(tokenizer.apply(text));
        }

        // Phase 4: cosmetic spacing.
        text = miscRule.enforceKeywordSpacing(tokenizer.apply(text));
        text = miscRule.enforceInitializerBraceSpacing(tokenizer.apply(text));
        text = miscRule.enforcePreIncrement(tokenizer.apply(text));
        if (isCpp) {
            text = cppRule.enforceTemplateAngleBracketSpacing(tokenizer.apply(text));
        }
        if (isCOrCpp && config.isFormatMacros()) {
            text = cppRule.alignMacroDefinitions(tokenizer.apply(text));
        }

        // Phase 5: file-header-level structure.
        if (isCOrCpp) {
            text = cppRule.enforceHeaderFileStructure(tokenizer.apply(text), filePath, config.isHeaderGuardRename());
        } else if (isJava) {
            text = javaRule.enforceImportOrdering(tokenizer.apply(text), config.javaImportOrder(),
                    config.isJavaImportSort(), config.javaImportDepth(), config.javaImportBlankLines());
        }

        // Phase 6: final whitespace normalization, last.
        text = miscRule.convertIndentation(tokenizer.apply(text), config.indentStyle());

        return text;
    }
}
