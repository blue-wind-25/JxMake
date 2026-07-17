/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.grid;

import java.util.Map;

public abstract class ModifierPriority {

    protected abstract Map<String, Integer> priorityMap();

    public final int priorityOf(final String modifier) {
        final Integer rank = priorityMap().get(modifier);
        return rank == null ? -1 : rank;
    }

    public final boolean isModifier(final String token) {
        return priorityMap().containsKey(token);
    }

    /** Number of fixed modifier columns for this language's grid model (max rank + 1). */
    public final int columnCount() {
        int max = -1;
        for (final int rank : priorityMap().values()) {
            max = Math.max(max, rank);
        }
        return max + 1;
    }
}
