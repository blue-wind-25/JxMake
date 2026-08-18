/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jxmake.formatter.rules.BashSpecificRule;

/**
 * STYLE_TOOLING.md §2 (Bash) dispatch. Standalone (not part of any existing family), same pattern
 * as {@link FormatterMakefile}/{@link FormatterToml}.
 */
public final class FormatterBash extends FormatterCore {

    // Interpreter basenames this formatter's fixed bash-grammar transform list is safe to run
    // against. Deliberately excludes "zsh" -- see README.md Known Limitations (Bash) and
    // STATE_TOOLING.md for the empirical evaluation behind this gate.
    private static final Set<String> BASH_COMPATIBLE_INTERPRETERS = new HashSet<>( Arrays.asList(
        "bash", "sh", "dash", "ksh"
    ) );

    private static final Pattern SHEBANG_LINE = Pattern.compile("^#!\\s*(\\S+)(?:\\s+(\\S+))?");

    public FormatterBash(final Lang lang)
    {
        super(lang);
    }

    @Override
    public String formatOne(
        final String  content,
        final String  filePath,
        final Config  config,
        final boolean formatOff
    )
    {
        if(formatOff) return content;
        if( !isBashCompatibleShebang(content) ) return content;

        final BashSpecificRule rule = new BashSpecificRule(
            config.indentSize(),
            config.isNormalizeCommentStartCase(),
            config.isNormalizeCommentEndPeriod()
        );
        rule.setNormalizeCommentMultiSentenceCase( config.isNormalizeCommentMultiSentenceCase() );

        return rule.format(content);
    }

    /**
     * Gate that runs before any rule-based formatting decides whether this content should be
     * touched at all. When a shebang line is present, its interpreter is authoritative: a
     * non-bash/sh/dash/ksh interpreter (most notably {@code zsh}) means this file may use dialect
     * syntax this formatter's fixed bash-grammar transform list can misparse (e.g. extended-glob
     * alternation under {@code pipeSpacing}), so formatting is skipped entirely. A
     * {@code #!/usr/bin/env <interp>} indirection is resolved to {@code <interp>}. Files with no
     * shebang at all (common for sourced helper scripts) fall through and are formatted as bash --
     * empirically the overwhelming majority of real-world shebang-less `.sh`/`.bash` content is
     * ordinary bash, and misformatting the rare non-bash exception is lower-stakes than silently
     * skipping most legitimate input (see STATE_TOOLING.md for the corpus evidence).
     */
    private static boolean isBashCompatibleShebang(final String content)
    {
        final Matcher m = SHEBANG_LINE.matcher(content);
        if( !m.find() ) return true;

        final String prog       = m.group(1);
        final String arg        = m.group(2);
        final String interpPath = ( prog.endsWith("env") && arg != null ) ? arg : prog;
        final int    slash      = interpPath.lastIndexOf('/');
        final String interpName = (slash >= 0) ? interpPath.substring(slash + 1) : interpPath;

        return BASH_COMPATIBLE_INTERPRETERS.contains(interpName);
    }

} // class FormatterBash
