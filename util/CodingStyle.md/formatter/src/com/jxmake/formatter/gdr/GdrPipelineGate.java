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
     * Safety cap on the number of GDR+formatOne cycles the convergence loop below will run before
     * giving up and failing loudly -- see {@code RDD_KEY_240}/{@code RDD_KEY_241}. The confirmed
     * adversarial counterexample (`RDD_KEY_240`) needed the equivalent of 4 cycles (two full old-
     * style 4-stage invocations chained) to reach a true fixed point; 20 is a wide margin above
     * that (5x), well beyond any known real or synthetic case, while still bounding a genuinely
     * pathological/non-converging input to a finite, fast failure instead of an infinite loop.
     */
    private static final int MAX_MULTIPASS_CYCLES = 20;

    /**
     * Single entry point covering both the base single-pre-pass-then-pipeline order and the
     * opt-in convergence-seeking multipass sequence (GDR, pipeline, repeated) -- see
     * {@code STATE_CURLY_GDR.md}'s "Open design proposal: bounded multi-pass remediation for
     * RDD_KEY_229" section, {@code RDD_KEY_233}/{@code RDD_KEY_234}, and {@code RDD_KEY_240}/
     * {@code RDD_KEY_241} for why a fixed 4-stage bound was replaced with an actual stability
     * check. `Main`'s standalone path and `ServerMode`'s request path both call this instead of
     * separately calling {@link #apply} and {@code FormatterCore.forLanguage(...).formatOne(...)},
     * so the multipass ordering lives in one place.
     *
     * <p>When {@code curly-general-scope-reindent} is off, or the language isn't curly-family,
     * this is exactly one {@code formatOne} call -- byte-for-byte the same as before this method
     * existed. When on but {@code curly-general-scope-reindent-multipass} is off, this is GDR
     * once then one {@code formatOne} call -- also unchanged from the prior behavior. Only when
     * both flags are on does the iterative cycle run (per {@code RDD_KEY_234}, the multipass flag
     * alone with the base flag off is a silent no-op, handled naturally here since {@link #apply}
     * itself already no-ops in that case).
     *
     * <p><b>Convergence mechanism (RDD_KEY_241):</b> repeats the GDR-pass + formatOne cycle,
     * comparing each new cycle's {@code formatOne} output against the immediately preceding
     * cycle's {@code formatOne} output, stopping as soon as two consecutive cycles produce a
     * byte-identical result (a true fixed point, not just "4 calls happened"). This replaces the
     * old unconditional 4-call sequence, which never compared any two stages' output and simply
     * assumed 4 calls was always enough -- an assumption {@code RDD_KEY_240} found a genuine
     * counterexample to. If {@link #MAX_MULTIPASS_CYCLES} is reached without convergence, this
     * fails loudly via {@link IllegalStateException} (surfaces as a per-file processing error to
     * the caller) rather than silently returning a possibly-still-oscillating result.
     */
    public static String applyAndFormat(
        String  source,
        String  language,
        Config  config,
        String  filePath,
        boolean formatOff
    )
    {
        final FormatterCore formatter = FormatterCore.forLanguage(language);

        final String gdr1      = apply(source, language, config);
        final String pipeline1 = formatter.formatOne(gdr1, filePath, config, formatOff);

        final boolean multipass = config.isCurlyGeneralScopeReindent() && config.isCurlyGeneralScopeReindentMultipass() && isCurlyFamily(
            language
        );
        if(!multipass) return pipeline1;

        String previous = pipeline1;
        for(int cycle = 2; cycle <= MAX_MULTIPASS_CYCLES; ++cycle) {
            final String gdrN      = apply(previous, language, config);
            final String pipelineN = formatter.formatOne(gdrN, filePath, config, formatOff);

            if( pipelineN.equals(previous) ) return pipelineN;

            previous = pipelineN;
        } // for

        throw new IllegalStateException(
            "GDR multipass reindentation failed to converge to a stable fixed point within "
                + MAX_MULTIPASS_CYCLES + " cycles for file: " + filePath
                + " -- curly-general-scope-reindent-multipass is oscillating rather than "
                + "stabilizing; this indicates a genuine non-convergent input, not a transient "
                + "or safe-to-ignore condition. See RDD_KEY_240/RDD_KEY_241 in RDD_LOG.md."
        );
    }

    private static boolean isCurlyFamily(String language)
    {
        return "c".equals(language) || "cpp".equals(language)
                || "java".equals(language) || "kotlin".equals(language)
                || "js".equals(language) || "ts".equals(language);
    }

} // class GdrPipelineGate
