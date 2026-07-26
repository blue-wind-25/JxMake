/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

public class M {
    void f() {
        Space space = whitespace();
        boolean lombokVal = source.startsWith("val", cursor);
        cursor += 3; // skip `val` or `var`
        typeExpr = new J.Identifier(randomId(),
                space,
                Markers.build(singletonList(JavaVarKeyword.build())),
                emptyList(),
                lombokVal ? "val" : "var",
                typeMapping.type(vartype),
                null);
    }
}
