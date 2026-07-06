/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.grid;

import java.util.HashMap;
import java.util.Map;

public class KotlinModifierPriority extends ModifierPriority {

    private static final Map<String, Integer> PRIORITY = new HashMap<>();

    static {
        PRIORITY.put("public", 0);
        PRIORITY.put("private", 0);
        PRIORITY.put("protected", 0);
        PRIORITY.put("internal", 0);
        // open/final/abstract/sealed share one column -- exactly one of these (or none) ever
        // applies to a given class/property/function, same reasoning already applied to
        // JavaModifierPriority's abstract/final/sealed slot (STYLE_JAVA17.md SS2 precedent).
        PRIORITY.put("open", 1);
        PRIORITY.put("final", 1);
        PRIORITY.put("abstract", 1);
        PRIORITY.put("sealed", 1);
        PRIORITY.put("override", 2);
        PRIORITY.put("const", 3);
        PRIORITY.put("lateinit", 4);
        // val/var share one slot per STYLE_KOTLIN.md SS6 -- they're mutually exclusive on any
        // single declaration (a property or parameter is either mutable or not, never both).
        PRIORITY.put("val", 5);
        PRIORITY.put("var", 5);
    }

    @Override
    protected Map<String, Integer> priorityMap() {
        return PRIORITY;
    }
}
