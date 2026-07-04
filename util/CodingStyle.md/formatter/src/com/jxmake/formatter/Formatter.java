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

public final class Formatter {
    private Formatter() {
    }

    public static String formatOne(final String content, final String language, final String filePath,
            final Config config) {
        final Lang lang = new Lang(language);
        final TokenizerCore tokenizer = new TokenizerCore(lang);
        final boolean isCpp = lang.isCpp;
        final boolean isC = lang.isC;
        final boolean isCOrCpp = isCpp || isC;
        final boolean isJava = lang.isJava;

        final BlockStructureRule blockRule = new BlockStructureRule(lang, config.closingCommentMinLines());
        final SwitchRule switchRule = new SwitchRule(lang);
        final MiscRule miscRule = new MiscRule(lang, config.isNormalizeCommentStartCase(),
                config.isNormalizeCommentEndPeriod());
        final CppSpecificRule cppRule = isCOrCpp ? new CppSpecificRule(lang) : null;
        final JavaSpecificRule javaRule = isJava ? new JavaSpecificRule(lang) : null;

        // Phase 0: §5/§6/§8/§14 grouping rules, recursive.
        String text = new ScopePipeline(lang, config.indentStyle(), config.isNormalizeCommentStartCase(),
                config.isNormalizeCommentEndPeriod()).process(content);

        // Phase 1: structural/brace passes.
        text = blockRule.collapseSingleExpressionBlocks(tokenizer.tokenize(text));
        text = blockRule.enforceKAndRBraceStyle(tokenizer.tokenize(text));
        text = blockRule.enforceNamedConstructHeaderSpacing(tokenizer.tokenize(text));
        text = blockRule.placeElseOnOwnLine(tokenizer.tokenize(text));
        text = blockRule.placeCatchFinallyOnOwnLine(tokenizer.tokenize(text));
        text = blockRule.insertNamedConstructBlankLines(tokenizer.tokenize(text));
        if (isCOrCpp) {
            text = cppRule.enforceFunctionDefinitionAllmanBraceStyle(tokenizer.tokenize(text));
            text = cppRule.enforceEmptyParameterList(tokenizer.tokenize(text));
            if (isCpp) {
                text = cppRule.enforceRequiresClausePlacement(tokenizer.tokenize(text));
            }
        } else if (isJava) {
            text = javaRule.enforceMethodDefinitionAllmanBraceStyle(tokenizer.tokenize(text));
            text = javaRule.enforcePermitsClauseLineBreaking(tokenizer.tokenize(text));
            text = javaRule.separateEnumConstantListTerminator(tokenizer.tokenize(text));
        }
        text = miscRule.enforceCallLineBreaking(tokenizer.tokenize(text));
        text = switchRule.formatNonInlineSwitches(tokenizer.tokenize(text));
        text = miscRule.insertBlankLineBeforeReturn(tokenizer.tokenize(text));

        // Phase 2: comment-style normalization -- must precede Phase 3 (RDD_KEY_47).
        text = miscRule.enforceCommentStyle(tokenizer.tokenize(text));
        text = miscRule.alignCommentSeparators(tokenizer.tokenize(text));

        // Phase 3: comment/marker-generating passes.
        // `alignInlineSwitches`/`markFallthrough` run before `addClosingComments` so the SWITCH
        // closing-comment line-count decision (STYLE.md §7) sees the switch body's final,
        // fully-compacted line count -- not its pre-alignment shape -- keeping the decision
        // (and thus idempotency) stable across repeated format passes.
        text = switchRule.markFallthrough(tokenizer.tokenize(text));
        text = switchRule.alignInlineSwitches(tokenizer.tokenize(text));
        text = blockRule.addClosingComments(tokenizer.tokenize(text));
        if (isJava) {
            text = javaRule.enforceSwitchExpressionArrowAlignment(tokenizer.tokenize(text));
        }

        // Phase 4: cosmetic spacing.
        text = miscRule.enforceKeywordSpacing(tokenizer.tokenize(text));
        text = miscRule.enforceComplexityPadding(tokenizer.tokenize(text));
        text = miscRule.enforceInitializerBraceSpacing(tokenizer.tokenize(text));
        text = miscRule.enforcePreIncrement(tokenizer.tokenize(text));
        if (isCpp) {
            text = cppRule.enforceTemplateAngleBracketSpacing(tokenizer.tokenize(text));
        }
        if (isCOrCpp && config.isFormatMacros()) {
            text = cppRule.alignMacroDefinitions(tokenizer.tokenize(text));
        }

        // Phase 5: file-header-level structure.
        if (isCOrCpp) {
            text = cppRule.enforceHeaderFileStructure(tokenizer.tokenize(text), filePath, config.isHeaderGuardRename());
        } else if (isJava) {
            text = javaRule.enforceImportOrdering(tokenizer.tokenize(text), config.javaImportOrder(),
                    config.isJavaImportSort(), config.javaImportDepth(), config.javaImportBlankLines());
        }

        // Phase 6: final whitespace normalization, last.
        text = miscRule.convertIndentation(tokenizer.tokenize(text), config.indentStyle());

        return text;
    }
}
