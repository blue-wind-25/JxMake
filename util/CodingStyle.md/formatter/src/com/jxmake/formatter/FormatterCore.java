/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter;

/**
 * Thin dispatcher/contract shared by every language-family formatter sibling
 * ({@link FormatterCurly} for C/C++/Java/Kotlin, {@link FormatterIndent} for Python3, and
 * {@link FormatterXml} for XML/HTML5). {@link #forLanguage} is the single place that
 * picks the right sibling by {@link Lang} family, so callers (`Main.java`/`ServerMode.java`) never
 * need their own if/else on language.
 */
public abstract class FormatterCore {

    protected final Lang lang;

    protected FormatterCore(final Lang lang)
    {
        this.lang = lang;
    }

    public abstract String formatOne(
        String content, String filePath, Config config, boolean formatOff
    );

    public static FormatterCore forLanguage(final String language)
    {
        return forLanguage(language, null);
    }

    /**
     * {@code filePath}-aware overload -- needed so {@link Lang#isJsxSyntax} (a `.jsx`/`.tsx`
     * extension check independent of the already-inferred `"js"`/`"ts"` language string, see
     * `Lang.infer`) can be set correctly per file. The path-less overload above is kept for
     * every caller with no real per-file path to check (e.g. `XmlSpecificRule`'s forced
     * `"js"`/`"css"` dispatch for embedded `<script>`/`<style>` content).
     */
    public static FormatterCore forLanguage(final String language, final String filePath)
    {
        return forLanguage(language, filePath, false);
    }

    /**
     * {@code jsxInTsOptIn}-aware overload -- threads {@code Config.isJsxInTs()} through to
     * {@link Lang}'s constructor so a `.ts` file can opt into the JSX boundary-finding pre-pass
     * (see `Lang.isJsxSyntax`'s javadoc, STATE_JS_TS.md's 2026-08-13 implementation section).
     * `.jsx`/`.tsx`/`.js`/`.mjs`/`.cjs` files are unaffected by this parameter either way.
     */
    public static FormatterCore forLanguage(
        final String  language,
        final String  filePath,
        final boolean jsxInTsOptIn
    )
    {
        final Lang lang = new Lang(language, filePath, jsxInTsOptIn);
        if(lang.isJson || lang.isJson5) return new FormatterJson(lang);
        if(lang.isCss) return new FormatterCss(lang);
        if(lang.isYaml) return new FormatterYaml(lang);
        if(lang.isToml) return new FormatterToml(lang);
        if(lang.isXml || lang.isHtml5) return new FormatterXml(lang);
        if(lang.isEini) return new FormatterEini(lang);
        if(lang.isJxMake) return new FormatterJxMake(lang);
        if(lang.isMakefile) return new FormatterMakefile(lang);
        if(lang.isBash) return new FormatterBash(lang);
        if(lang.isPowerShell) return new FormatterPowerShell(lang);
        if(lang.isCurly) return new FormatterCurly(lang);
        if(lang.isIndentBased) return new FormatterIndent(lang);
        throw new UnsupportedOperationException("'" + language + "' has no formatter dispatch yet");
    }

} // class FormatterCore
