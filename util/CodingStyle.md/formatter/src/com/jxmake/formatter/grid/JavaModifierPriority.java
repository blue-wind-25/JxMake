/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.grid;

import java.util.HashMap;
import java.util.Map;

public class JavaModifierPriority extends ModifierPriority {

    private static final Map<String, Integer> PRIORITY = new HashMap<>();

    static {
        PRIORITY.put("public", 0);
        PRIORITY.put("private", 0);
        PRIORITY.put("protected", 0);
        PRIORITY.put("static", 1);
        PRIORITY.put("volatile", 2);
        PRIORITY.put("final", 3);
    }

    @Override
    protected Map<String, Integer> priorityMap() {
        return PRIORITY;
    }
}
