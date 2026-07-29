/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
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
        // abstract/final/sealed share one column -- per-kind, exactly one of these (or none) ever
        // applies to a given class/method/field, so the shared rank is never ambiguous in practice
        // (STYLE_JAVA17.md SS2, resolved). `non-sealed` is semantically in this same slot but has no
        // entry here: it lexes as three tokens (`non` `-` `sealed`), not one, so it can't be a single
        // map key -- any future class-modifier-reordering pass must special-case it as a unit.
        PRIORITY.put("abstract", 2);
        PRIORITY.put("final", 2);
        PRIORITY.put("sealed", 2);
        PRIORITY.put("volatile", 3);
    }

    @Override
    protected Map<String, Integer> priorityMap()
    {
        return PRIORITY;
    }

} // class JavaModifierPriority
