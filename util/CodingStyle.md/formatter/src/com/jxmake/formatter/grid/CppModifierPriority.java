/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.grid;

import java.util.HashMap;
import java.util.Map;

public class CppModifierPriority extends ModifierPriority {

    private static final Map<String, Integer> PRIORITY = new HashMap<>();

    static {
        // Typedef always leads any other specifier (C/C++ grammar requires it first)
        PRIORITY.put("typedef", 0);
        PRIORITY.put("static", 1);
        // constexpr/consteval/constinit share one column -- mutually exclusive, exactly one
        // (or none) ever applies to a given declaration (STYLE_CPP20.md SS3, same shared-rank
        // precedent as JavaModifierPriority's abstract/final/sealed).
        PRIORITY.put("constexpr", 2);
        PRIORITY.put("consteval", 2);
        PRIORITY.put("constinit", 2);
        PRIORITY.put("volatile", 3);
        PRIORITY.put("const", 4);
        // virtual/inline/explicit are function-declaration qualifiers; they share one column
        // since they are mutually exclusive in practice (a single function is at most one)
        PRIORITY.put("virtual", 5);
        PRIORITY.put("inline", 5);
        PRIORITY.put("explicit", 5);
    }

    @Override
    protected Map<String, Integer> priorityMap()
    {
        return PRIORITY;
    }

} // class CppModifierPriority
