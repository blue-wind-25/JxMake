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
        String text = new ScopePipeline(lang, config.indentStyle(), config.isNormalizeCommentStartCase(),
                config.isNormalizeCommentEndPeriod(), formatOff).process(content);

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
        text = miscRule.enforceCallLineBreaking(tokenizer.apply(text));
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
        text = miscRule.enforceComplexityPadding(tokenizer.apply(text));
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
