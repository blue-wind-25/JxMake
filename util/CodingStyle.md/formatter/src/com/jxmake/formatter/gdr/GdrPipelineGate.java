package com.jxmake.formatter.gdr;

import com.jxmake.formatter.Config;
import com.jxmake.formatter.FormatterCore;

/**
 * The single decision point for whether the GDR pre-pass runs ahead of the existing formatter
 * pipeline -- see {@code STATE_CURLY_GDR.md}'s checklist item 6. Both `Main`'s standalone path
 * and `ServerMode`'s request path call {@link #apply} in the same place, right before handing
 * source text to {@code FormatterCore.forLanguage(language).formatOne(...)}, so neither entry
 * point encodes the gating logic itself.
 *
 * <p>Deliberately lives outside both `com.jxmake.formatter.gdr`'s own reindenter classes are
 * already isolated, and outside the existing pipeline packages entirely -- when
 * {@code curly-general-scope-reindent} is off (the default) or the language isn't one of the
 * curly-brace family this pre-pass targets, {@link #apply} returns {@code source} completely
 * unchanged (same reference), so the existing pipeline's own behavior is untouched.
 */
public final class GdrPipelineGate {

    private GdrPipelineGate()
    {
    }

    public static String apply(String source, String language, Config config)
    {
        if( !config.isCurlyGeneralScopeReindent() ) return source;
        if( !isCurlyFamily(language) ) return source;

        return GdrRewriter.rewrite( source, config.indentSize() );
    }

    /**
     * Single entry point covering both the base single-pre-pass-then-pipeline order and the
     * opt-in 4-stage multipass sequence (GDR, pipeline, GDR, pipeline) -- see
     * {@code STATE_CURLY_GDR.md}'s "Open design proposal: bounded multi-pass remediation for
     * RDD_KEY_229" section, {@code RDD_KEY_233}/{@code RDD_KEY_234}. `Main`'s standalone path and
     * `ServerMode`'s request path both call this instead of separately calling {@link #apply} and
     * {@code FormatterCore.forLanguage(...).formatOne(...)}, so the 4-stage ordering lives in one
     * place.
     *
     * <p>When {@code curly-general-scope-reindent} is off, or the language isn't curly-family,
     * this is exactly one {@code formatOne} call -- byte-for-byte the same as before this method
     * existed. When on but {@code curly-general-scope-reindent-multipass} is off, this is GDR
     * once then one {@code formatOne} call -- also unchanged from the prior behavior. Only when
     * both flags are on does the 4-stage sequence run (per {@code RDD_KEY_234}, the multipass flag
     * alone with the base flag off is a silent no-op, handled naturally here since {@link #apply}
     * itself already no-ops in that case).
     */
    public static String applyAndFormat(
        String source, String language, Config config, String filePath, boolean formatOff
    )
    {
        final FormatterCore formatter = FormatterCore.forLanguage(language);

        final String gdr1       = apply(source, language, config);
        final String pipeline1  = formatter.formatOne(gdr1, filePath, config, formatOff);

        final boolean multipass = config.isCurlyGeneralScopeReindent()
                && config.isCurlyGeneralScopeReindentMultipass()
                && isCurlyFamily(language);
        if( !multipass ) return pipeline1;

        final String gdr2      = apply(pipeline1, language, config);
        final String pipeline2 = formatter.formatOne(gdr2, filePath, config, formatOff);

        return pipeline2;
    }

    private static boolean isCurlyFamily(String language)
    {
        return "c".equals(language) || "cpp".equals(language)
                || "java".equals(language) || "kotlin".equals(language)
                || "js".equals(language) || "ts".equals(language);
    }

} // class GdrPipelineGate
