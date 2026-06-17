/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.grid;

import java.util.Map;

public abstract class ModifierPriority {

    protected abstract Map<String, Integer> priorityMap();

    public final int priorityOf(String modifier) {
        Integer rank = priorityMap().get(modifier);
        return rank == null ? -1 : rank;
    }

    public final boolean isModifier(String token) {
        return priorityMap().containsKey(token);
    }
}
