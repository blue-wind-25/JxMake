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
        // CUDA qualifiers, each its own column: `__host__ __device__` both apply to one function,
        // and two modifiers sharing a rank would overwrite each other's cell
        PRIORITY.put("__global__", 2);
        PRIORITY.put("__host__", 3);
        PRIORITY.put("__device__", 4);
        PRIORITY.put("__shared__", 5);
        PRIORITY.put("__constant__", 6);
        PRIORITY.put("__managed__", 7);
        // constexpr/consteval/constinit share one column -- mutually exclusive, exactly one
        // (or none) ever applies to a given declaration (STYLE_CPP20.md SS3, same shared-rank
        // precedent as JavaModifierPriority's abstract/final/sealed)
        PRIORITY.put("constexpr", 8);
        PRIORITY.put("consteval", 8);
        PRIORITY.put("constinit", 8);
        PRIORITY.put("volatile", 9);
        PRIORITY.put("const", 10);
        // virtual/inline/explicit are function-declaration qualifiers; they share one column
        // since they are mutually exclusive in practice (a single function is at most one)
        PRIORITY.put("virtual", 11);
        PRIORITY.put("inline", 11);
        PRIORITY.put("explicit", 11);
        // CUDA inlining hints, mutually exclusive with each other
        PRIORITY.put("__forceinline__", 12);
        PRIORITY.put("__noinline__", 12);
    }

    @Override
    protected Map<String, Integer> priorityMap()
    {
        return PRIORITY;
    }

} // class CppModifierPriority
