/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

/**
 * Shared helper logic that was structurally identical, byte-for-byte, across {@link
 * MakefileSpecificRule}, {@link BashSpecificRule}, and {@link PowerShellSpecificRule} (2026-08-16
 * cleanup-pass consolidation, mirroring the {@code YamlTomlSharedRule} pattern already used for
 * YAML/TOML): a negative-clamping {@code repeatChar} and the {@code indent(depth)} built on top of
 * it. Not merged with {@code YamlTomlSharedRule}/{@link com.jxmake.formatter.FormatterSimpleBraced}
 * because those two families' own {@code repeatChar} does not clamp a negative {@code count}
 * (callers there never pass one) -- kept as a separate, intentionally-narrower helper rather than
 * force one shared implementation onto both clamping conventions.
 */
final class ToolingSharedRule {

    private ToolingSharedRule()
    {
    }

    static String repeatChar(final char c, final int count)
    {
        final StringBuilder sb = new StringBuilder( Math.max(0, count) );
        for(int i = 0; i < count; ++i) sb.append(c);

        return sb.toString();
    }

    static String indent(final int depth, final int indentWidth)
    {
        return repeatChar( ' ', Math.max(0, depth) * indentWidth );
    }

} // class ToolingSharedRule
