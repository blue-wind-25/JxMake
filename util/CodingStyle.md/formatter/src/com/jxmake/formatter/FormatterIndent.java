/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

/**
 * Indentation-block-family formatter (Python3 -- see STATE_PYTHON3.md). Currently a statement/
 * indentation skeleton only -- delegates straight to {@link ScopePipelineIndent}'s identity
 * tokenize/render round-trip, no STYLE_PYTHON3.md §1-9 cosmetic rules applied yet. Reachable only
 * via direct construction/test harnesses today: `Main.run`/`ServerMode` both gate on
 * {@link Lang#isScaffoldOnly} and throw {@link UnsupportedLanguageException} before ever
 * constructing this class for a real `--lang python3` invocation, consistent with every other
 * language's precedent in this codebase (a language leaves {@link Lang#SCAFFOLD_ONLY_LANGUAGES}
 * only once it has real, substantive formatting logic -- not merely a lossless skeleton). Mirrors
 * {@link FormatterJson}'s whole-file `formatOff` short-circuit (Python has no per-region frozen-
 * span mechanism decided/implemented yet, so there is nothing narrower to honor).
 */
public final class FormatterIndent extends FormatterCore {

    public FormatterIndent(final Lang lang) {
        super(lang);
    }

    @Override
    public String formatOne(final String content, final String filePath, final Config config,
            final boolean formatOff) {
        if (formatOff) {
            return content;
        }
        return new ScopePipelineIndent(lang, config.indentSize(), config.lineLength()).process(content);
    }
}
