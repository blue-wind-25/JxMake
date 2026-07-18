/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import java.util.Locale;

/**
 * Precomputes the `"c"`/`"cpp"`/`"java"`/`"kotlin"` language identity of the file being formatted
 * exactly once per {@link FormatterCore#formatOne}, so rule classes read {@link #isC}/{@link #isCpp}/
 * {@link #isJava}/{@link #isKotlin} instead of each re-comparing the raw {@link #language} string.
 *
 * <p>Also recognizes ("dispatches") a second tier of languages that are wired into detection but
 * have no real formatting logic yet -- {@link #isJson}/{@link #isJson5}/{@link #isYaml}/
 * {@link #isToml}, {@link #isJs}/{@link #isTs}, {@link #isPython3}. These are
 * intentionally excluded from {@link #isSupported}/{@link #SUPPORTED_LANGUAGES} (the `--lang`
 * CLI flag / server `lang` param whitelist) so a caller can never construct a {@link FormatterCore}
 * pipeline for them; {@link #isScaffoldOnly} is the single source of truth callers use to route
 * a recognized-but-unimplemented language to {@link UnsupportedLanguageException} instead -- see
 * `Main.processFile`/`ServerMode.FormatHandler.handle`.
 *
 * <p>{@link #isCurly}/{@link #isIndentBased}/{@link #isTagBased} classify by scoping-delimiter
 * family (brace-block, indentation-block, tag-nested) -- used to pick the right `*Curly`/
 * `*Indent`/`*Tags` sibling class in `TokenizerCore`/`FormatterCore`/`ScopePipelineCore`/`MiscRuleCore` and
 * friends. JSON/JSON5/YAML/TOML/CSS are none of the three.
 */
public final class Lang {
    public final String language;
    public final boolean isC;
    public final boolean isCpp;
    public final boolean isJava;
    public final boolean isKotlin;
    public final boolean isJson;
    public final boolean isJson5;
    public final boolean isYaml;
    public final boolean isToml;
    public final boolean isXml;
    public final boolean isCss;
    public final boolean isHtml5;
    public final boolean isJs;
    public final boolean isTs;
    public final boolean isPython3;
    public final boolean isCurly;
    public final boolean isIndentBased;
    public final boolean isTagBased;
    public final boolean isSimpleBraced;

    public Lang(final String language) {
        this.language = language;
        this.isC = "c".equals(language);
        this.isCpp = "cpp".equals(language);
        this.isJava = "java".equals(language);
        this.isKotlin = "kotlin".equals(language);
        this.isJson = "json".equals(language);
        this.isJson5 = "json5".equals(language);
        this.isYaml = "yaml".equals(language);
        this.isToml = "toml".equals(language);
        this.isXml = "xml".equals(language);
        this.isCss = "css".equals(language);
        this.isHtml5 = "html5".equals(language);
        this.isJs = "js".equals(language);
        this.isTs = "ts".equals(language);
        this.isPython3 = "python3".equals(language);
        this.isCurly = isC || isCpp || isJava || isKotlin || isJs || isTs;
        this.isIndentBased = isPython3;
        this.isTagBased = isXml || isHtml5;
        // JSON/JSON5 + CSS: brace/bracket-delimited like the curly family, but no imperative
        // control flow (no keywords, no preprocessor, no functions) -- share a distinct
        // "SimpleBraced" family (`TokenizerSimpleBraced`/`FormatterSimpleBraced`) instead of either
        // `*Curly` or the flat, indentation-only family reserved for YAML/TOML.
        this.isSimpleBraced = isJson || isJson5 || isCss;
    }

    /* When updating the supported language here, also update the language/extension list in:
     *    the {@link Lang} constructor above (isC/isCpp/isJava/isKotlin)
     *    the `--lang` validation in `Main.run()`
     *    `ServerMode.FormatHandler.handle()`
     */
    public static final String SUPPORTED_LANGUAGES = "c, cpp, java, kotlin, json, json5";

    /**
     * Scaffold-only languages: recognized by {@link #infer} and accepted by `--lang`/`lang=`, but
     * every real formatting attempt throws {@link UnsupportedLanguageException} -- no rule classes
     * exist for them yet. See `STATE_DATA_FORMATS.md`/`STATE_JS_TS.md`/`STATE_PYTHON3.md`. (C++26
     * is deliberately NOT a separate scaffold entry here -- it is future incremental rule coverage
     * on the existing, already-implemented {@code "cpp"} pipeline, the same way C++20 support was
     * folded in with no separate {@code isCpp20}/{@code --lang cpp20} selector. See RDD_KEY_180,
     * which supersedes RDD_KEY_179's now-reverted separate-language approach.) JSON/JSON5 moved out
     * of this list once `FormatterJson`/`JsonSpecificRule` landed real logic -- see RDD_KEY_190.
     */
    public static final String SCAFFOLD_ONLY_LANGUAGES =
            "yaml, toml, xml, css, html5, js, ts, python3";

    public static boolean isSupported(final String language) {
        return "c".equals(language) || "cpp".equals(language)
                || "java".equals(language) || "kotlin".equals(language)
                || "json".equals(language) || "json5".equals(language);
    }

    public static boolean isScaffoldOnly(final String language) {
        return "yaml".equals(language) || "toml".equals(language) || "xml".equals(language)
                || "css".equals(language) || "html5".equals(language) || "js".equals(language)
                || "ts".equals(language) || "python3".equals(language);
    }

    /** {@code isSupported || isScaffoldOnly} -- every language recognized by this codebase at all. */
    public static boolean isRecognized(final String language) {
        return isSupported(language) || isScaffoldOnly(language);
    }

    public static String infer(final String path) {
        final String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".java")) {
            return "java";
        }
        if (lower.endsWith(".c") || lower.endsWith(".h")) {
            return "c";
        }
        if (lower.endsWith(".cc") || lower.endsWith(".cpp") || lower.endsWith(".cxx")
                || lower.endsWith(".hh") || lower.endsWith(".hpp") || lower.endsWith(".hxx")) {
            return "cpp";
        }
        if (lower.endsWith(".kt") || lower.endsWith(".kts")) {
            return "kotlin";
        }
        if (lower.endsWith(".json5")) {
            return "json5";
        }
        if (lower.endsWith(".json")) {
            return "json";
        }
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) {
            return "yaml";
        }
        if (lower.endsWith(".toml")) {
            return "toml";
        }
        if (lower.endsWith(".xml")) {
            return "xml";
        }
        if (lower.endsWith(".css")) {
            return "css";
        }
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return "html5";
        }
        // .jsx folds into "js" and .tsx into "ts" for detection/dispatch purposes this session --
        // JSX/TSX need their own future embedding-aware dispatcher (STATE_JS_TS.md Open Design
        // Questions), not a distinct Lang flag yet; both are scaffold-only either way.
        if (lower.endsWith(".jsx") || lower.endsWith(".mjs") || lower.endsWith(".cjs")
                || lower.endsWith(".js")) {
            return "js";
        }
        if (lower.endsWith(".tsx") || lower.endsWith(".ts")) {
            return "ts";
        }
        if (lower.endsWith(".py")) {
            return "python3";
        }
        return null;
    }
}
