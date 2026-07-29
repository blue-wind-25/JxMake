/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.classifier;

/**
 * RDD_KEY_95: presence-based gate, not ratio-based -- any non-Latin codepoint anywhere in the
 *  comment skips the classifier entirely for that comment (mixed-token comments defeat
 *  whole-string language-ID; this is permanently out of scope, not a threshold to tune)
 */
public final class NonLatinScriptGate {

    private NonLatinScriptGate()
    {
    }

    public static boolean containsNonLatinScript(final String commentText)
    {
        int i = 0;
        while( i < commentText.length() ) {
            final int codepoint = commentText.codePointAt(i);
            i += Character.charCount(codepoint);
            final Character.UnicodeScript script = Character.UnicodeScript.of(codepoint);
            if(script != Character.UnicodeScript.LATIN && script != Character.UnicodeScript.COMMON && script != Character.UnicodeScript.INHERITED) return true;
        }

        return false;
    }

} // class NonLatinScriptGate
