package com.jxmake.formatter.gdr;

import com.jxmake.formatter.Config;

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

    private GdrPipelineGate() {
    }

    public static String apply(String source, String language, Config config) {
        if (!config.isCurlyGeneralScopeReindent()) {
            return source;
        }
        if (!isCurlyFamily(language)) {
            return source;
        }
        return GdrRewriter.rewrite(source, config.indentSize());
    }

    private static boolean isCurlyFamily(String language) {
        return "c".equals(language) || "cpp".equals(language)
                || "java".equals(language) || "kotlin".equals(language);
    }
}
