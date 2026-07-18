/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import java.util.List;

/**
 * Shared base for the "SimpleBraced" family (RDD_KEY_190): brace/bracket-delimited formats with no
 * imperative control flow -- JSON/JSON5 ({@link FormatterJson}) and, eventually, CSS. Holds
 * {@link #padKeysForColonAlignment}, the group-column-padding computation shared by
 * STYLE_DATA_FORMATS.md §1.1 (JSON key/value colon alignment) and §3.1 (CSS property/value colon
 * alignment) -- both describe the identical "pad so `:` lines up, space always precedes `:`"
 * shape.
 */
public abstract class FormatterSimpleBraced extends FormatterCore {

    protected FormatterSimpleBraced(final Lang lang) {
        super(lang);
    }

    /** Returns, for each key in {@code keys} (a single alignment group), the padding spaces to
     *  insert between that key and its `:` so every `:` in the group lines up at the same column
     *  -- the widest key in the group gets exactly one space, every other key gets enough extra
     *  padding to match. */
    public static String[] padKeysForColonAlignment(final List<String> keys) {
        int widest = 0;
        for (final String key : keys) {
            widest = Math.max(widest, key.length());
        }
        final String[] padded = new String[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            final int spaces = widest - keys.get(i).length() + 1;
            final StringBuilder sb = new StringBuilder();
            for (int s = 0; s < spaces; s++) {
                sb.append(' ');
            }
            padded[i] = sb.toString();
        }
        return padded;
    }
}
