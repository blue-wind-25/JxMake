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
 * <p>{@link #isPython3} is a fully implemented language like every other -- included in
 * {@link #isSupported}/{@link #SUPPORTED_LANGUAGES} (the `--lang` CLI flag / server `lang` param
 * whitelist). {@link #isScaffoldOnly} currently always returns {@code false}: every language this
 * codebase recognizes has real formatting logic. {@link #isJs}/{@link #isTs} moved out of
 * scaffold-only once `JsTsSpecificRule`/`JsTsDeclarationAlignmentRule` landed real logic -- see
 * `STATE_JS_TS.md`; {@link #isPython3} moved out once `FormatterIndent`/`ScopePipelineIndent`
 * landed real logic -- see `STATE_PYTHON3.md`.
 *
 * <p>{@link #isCurly}/{@link #isIndentBased}/{@link #isTagBased} classify by scoping-delimiter
 * family (brace-block, indentation-block, tag-nested) -- used to pick the right `*Curly`/
 * `*Indent`/`*Tags` sibling class in `TokenizerCore`/`FormatterCore`/`ScopePipelineCore`/`MiscRuleCore` and
 * friends. JSON/JSON5/YAML/TOML/CSS are none of the three.
 */
public final class Lang {

    public final String  language;
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
    public final boolean isMakefile;
    public final boolean isBash;
    public final boolean isPowerShell;
    public final boolean isCurly;
    public final boolean isIndentBased;
    public final boolean isTagBased;
    public final boolean isSimpleBraced;
    /**
     * True when the JSX boundary-finding pre-pass ({@code TokenizerCurly#findJsxSpans}) should run
     *  for this file. Gates the pre-pass -- a file this is {@code false} for must see zero behavior
     *  change, so the pre-pass must never run unless this is true.
     *
     *  <p>Extension-based rules (STATE_JS_TS.md's 2026-08-13 implementation section, following the
     *  2026-08-13 research session's recommendation, itself following Babel/Prettier's own
     *  precedent):
     *  <ul>
     *    <li>{@code .jsx}/{@code .tsx} -- always {@code true} (unchanged from the original
     *        Step-1 landing).</li>
     *    <li>{@code .js}/{@code .mjs}/{@code .cjs} -- always {@code true} (widened 2026-08-13):
     *        plain JS has no competing ambiguous syntax (the legacy `<Type>value` angle-bracket
     *        cast is TS-only grammar), so this mirrors Babel/Prettier's own default-on behavior for
     *        the whole JS-family bucket.</li>
     *    <li>{@code .ts} -- {@code false} by default (deliberately NOT widened, mirroring `tsc`'s/
     *        Prettier's own `.ts`-vs-`.tsx` split -- `.ts` is exactly where the legacy `<Type>value`
     *        cast collision is real and non-rare), {@code true} only when the caller explicitly
     *        opts in via {@code jsxInJsOptIn} (the {@code jsx-in-js} config key, see
     *        `Config.isJsxInJs`).</li>
     *    <li>Anything else -- {@code false}.</li>
     *  </ul>
     *
     *  <p>Defaults to {@code false} via the path-less constructor (used by every non-file-scoped
     *  `Lang` caller, e.g. `XmlSpecificRule`'s forced `"js"`/`"css"` dispatch for embedded
     *  `<script>`/`<style>` content, which has no real file path to check) and via the two-arg
     *  constructor (opt-in defaults to off when not explicitly threaded through).
     */
    public final boolean isJsxSyntax;

    public Lang(final String language)
    {
        this(language, null);
    }

    public Lang(final String language, final String filePath)
    {
        this(language, filePath, false);
    }

    /**
     * {@code jsxInJsOptIn}-aware overload -- lets a caller thread {@code Config.isJsxInJs()}
     *  through so a {@code .ts} file can opt into the JSX boundary-finding pre-pass (see
     *  {@link #isJsxSyntax}'s javadoc). Has no effect on any extension other than {@code .ts} --
     *  {@code .jsx}/{@code .tsx}/{@code .js}/{@code .mjs}/{@code .cjs} are unaffected either way.
     */
    public Lang(final String language, final String filePath, final boolean jsxInJsOptIn)
    {
        this.isJsxSyntax    = filePath != null && isJsxSyntaxPath(filePath, jsxInJsOptIn);
        this.language       = language;
        this.isC            = "c".equals(language);
        this.isCpp          = "cpp".equals(language);
        this.isJava         = "java".equals(language);
        this.isKotlin       = "kotlin".equals(language);
        this.isJson         = "json".equals(language);
        this.isJson5        = "json5".equals(language);
        this.isYaml         = "yaml".equals(language);
        this.isToml         = "toml".equals(language);
        this.isXml          = "xml".equals(language);
        this.isCss          = "css".equals(language);
        this.isHtml5        = "html5".equals(language);
        this.isJs           = "js".equals(language);
        this.isTs           = "ts".equals(language);
        this.isPython3      = "python3".equals(language);
        this.isMakefile     = "makefile".equals(language);
        this.isBash         = "bash".equals(language);
        this.isPowerShell   = "powershell".equals(language);
        this.isCurly        = isC || isCpp || isJava || isKotlin || isJs || isTs;
        this.isIndentBased  = isPython3;
        this.isTagBased     = isXml || isHtml5;
        this.isSimpleBraced = isJson || isJson5 || isCss;
    }

    private static boolean isJsxSyntaxPath(final String filePath, final boolean jsxInJsOptIn)
    {
        final String lower = filePath.toLowerCase(Locale.ROOT);

        if( lower.endsWith(".jsx") || lower.endsWith(".tsx") ) return true;
        if( lower.endsWith(
            ".js"
        ) || lower.endsWith(
            ".mjs"
        ) || lower.endsWith(".cjs") ) return true;
        if( lower.endsWith(".ts") ) return jsxInJsOptIn;

        return false;
    }

    /*
     * When updating the supported language here, also update the language/extension list in:
     *    the {@link Lang} constructor above (isC/isCpp/isJava/isKotlin)
     *    the `--lang` validation in `Main.run()`
     *    `ServerMode.FormatHandler.handle()`
     */
    public static final String SUPPORTED_LANGUAGES = "c, cpp, java, kotlin, json, json5, css, yaml, toml, xml, html5, js, ts, python3, makefile, bash, powershell";

    /**
     * Scaffold-only languages: recognized by {@link #infer} and accepted by `--lang`/`lang=`, but
     * every real formatting attempt throws {@link UnsupportedLanguageException} -- no rule classes
     * exist for them yet. (C++26 is deliberately NOT a separate scaffold
     * entry here -- it is future incremental rule coverage on the existing, already-implemented
     * {@code "cpp"} pipeline, the same way C++20 support was folded in with no separate
     * {@code isCpp20}/{@code --lang cpp20} selector. See RDD_KEY_180, which supersedes
     * RDD_KEY_179's now-reverted separate-language approach.) JSON/JSON5/CSS moved out of this
     * list once `FormatterJson`/`JsonSpecificRule` and `FormatterCss`/`CssSpecificRule` landed
     * real logic -- see RDD_KEY_190. YAML/TOML moved out once `FormatterYaml`/`YamlSpecificRule`
     * and `FormatterToml`/`TomlSpecificRule` landed real logic. XML and HTML5 moved out once
     * `FormatterXml`/`XmlSpecificRule` landed real logic for both (they share the same class
     * internally, gated on `lang.isHtml5` -- RDD_KEY_188). JS/TS moved out once
     * `JsTsSpecificRule`/`JsTsDeclarationAlignmentRule` landed real logic -- see `STATE_JS_TS.md`;
     * HTML5's `<script>` dispatch to JS/TS (`XmlSpecificRule.renderScriptOrStyle`) now formats
     * real (non-frozen) script content for real rather than throwing. Python3 moved out once
     * `FormatterIndent`/`ScopePipelineIndent` landed real logic for §1-9 of `STYLE_PYTHON3.md`
     * -- see `STATE_PYTHON3.md`. This list is now empty; kept as a `String` constant (rather than
     * removed outright) since `Main.java`/`ServerMode.java` still reference it in usage/error text
     * for a future scaffold-only language to reuse.
     */
    public static final String SCAFFOLD_ONLY_LANGUAGES = "";

    public static boolean isSupported(final String language)
    {
        return "c".equals(language) || "cpp".equals(language)
                || "java".equals(language) || "kotlin".equals(language)
                || "json".equals(language) || "json5".equals(language)
                || "css".equals(language) || "yaml".equals(language) || "toml".equals(language)
                || "xml".equals(language) || "html5".equals(language)
                || "js".equals(language) || "ts".equals(language)
                || "python3".equals(
                    language
                ) || "makefile".equals(
                    language
                ) || "bash".equals(
                    language
                )
                || "powershell".equals(language);
    }

    public static boolean isScaffoldOnly(final String language)
    {
        return false;
    }

    /** {@code isSupported || isScaffoldOnly} -- every language recognized by this codebase at all */
    public static boolean isRecognized(final String language)
    {
        return isSupported(language) || isScaffoldOnly(language);
    }

    public static String infer(final String path)
    {
        final String lower = path.toLowerCase(Locale.ROOT);
        final int    slash = Math.max( lower.lastIndexOf('/'), lower.lastIndexOf('\\') );
        final String base  = slash < 0 ? lower : lower.substring(slash + 1);
        if( base.equals(
            "makefile"
        ) || base.equals(
            "gnumakefile"
        ) || base.endsWith(
            ".mk"
        ) ) return "makefile";
        if( lower.endsWith(".java") ) return "java";
        if( lower.endsWith(".c") || lower.endsWith(".h") ) return "c";
        if( lower.endsWith(
            ".cc"
        ) || lower.endsWith(
            ".cpp"
        ) || lower.endsWith(
            ".cxx"
        ) || lower.endsWith(
            ".hh"
        ) || lower.endsWith(
            ".hpp"
        ) || lower.endsWith(
            ".hxx"
        ) ) return "cpp";
        if( lower.endsWith(".kt") || lower.endsWith(".kts") ) return "kotlin";
        if( lower.endsWith(".json5") ) return "json5";
        if( lower.endsWith(".json") ) return "json";
        if( lower.endsWith(".yaml") || lower.endsWith(".yml") ) return "yaml";
        if( lower.endsWith(".toml") ) return "toml";
        if( lower.endsWith(
            ".xml"
        ) || lower.endsWith(
            ".svg"
        ) || lower.endsWith(
            ".xsd"
        ) || lower.endsWith(
            ".xsl"
        ) ) return "xml";
        if( lower.endsWith(".css") ) return "css";
        if( lower.endsWith(".html") || lower.endsWith(".htm") ) return "html5";
        // .jsx folds into "js" and .tsx into "ts" for detection/dispatch purposes this session --
        // JSX/TSX need their own future embedding-aware dispatcher (STATE_JS_TS.md Open Design
        // Questions), not a distinct Lang flag yet; both are scaffold-only either way.
        if( lower.endsWith(
            ".js"
        ) || lower.endsWith(
            ".jsx"
        ) || lower.endsWith(
            ".mjs"
        ) || lower.endsWith(
            ".cjs"
        ) ) return "js";
        if( lower.endsWith(".ts") || lower.endsWith(".tsx") ) return "ts";
        if( lower.endsWith(".py") ) return "python3";
        if( lower.endsWith(".sh") || lower.endsWith(".bash") ) return "bash";
        if( lower.endsWith(".ps1") || lower.endsWith(".psm1") ) return "powershell";

        return null;
    }

} // class Lang
