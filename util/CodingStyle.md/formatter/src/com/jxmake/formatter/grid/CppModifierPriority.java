/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.grid;

import java.util.HashMap;
import java.util.Map;

public class CppModifierPriority extends ModifierPriority {

    private static final Map<String, Integer> PRIORITY = new HashMap<>();

    static {
        PRIORITY.put("static", 0);
        // constexpr/consteval/constinit share one column -- mutually exclusive, exactly one
        // (or none) ever applies to a given declaration (STYLE_CPP20.md SS3, same shared-rank
        // precedent as JavaModifierPriority's abstract/final/sealed).
        PRIORITY.put("constexpr", 1);
        PRIORITY.put("consteval", 1);
        PRIORITY.put("constinit", 1);
        PRIORITY.put("volatile", 2);
        PRIORITY.put("const", 3);
        // virtual/inline/explicit are function-declaration qualifiers; they share one column
        // since they are mutually exclusive in practice (a single function is at most one).
        PRIORITY.put("virtual", 4);
        PRIORITY.put("inline", 4);
        PRIORITY.put("explicit", 4);
    }

    @Override
    protected Map<String, Integer> priorityMap() {
        return PRIORITY;
    }
}
