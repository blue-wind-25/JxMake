/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
package com.example;

public class RealCodeRegressions193 {

    private static void evaluate(String s)
    {
        s = applyKeywordParenSpacing(
            s
        ); // §3.5 (also benefits if/while/... examples in §3.1)
        s = applyOperatorSpacing(s);                                                                   // §3.2 spacing
        s = applyExceptionallyLongMethodNameThatForcesEnforceCallLineBreakingToWrapThisOneCallOnly(s); // §3.6
        s = applyBraceIndent(
            s
        ); // §3.1 (also multi-line hashtable bodies -- §3.4)
        s = applyPipelineSplit(
            s
        ); // §3.3 (after indent so continuation uses base+1 level)
        s = applyAssignAlignment(
            s
        ); // §3.2 alignment (also multi-line hashtable entries -- §3.4)
        s = applySwitchArmAlignment(
            s
        ); // §3.5 arm `{` alignment (after indent)
    }

} // class RealCodeRegressions193
