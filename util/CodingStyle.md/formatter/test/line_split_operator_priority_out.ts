/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

//%JXM_CFMT_CFG line-split-operator-priority=on

function optionalChainAndNullishNeverMistakenForTernary(x: { y?: { z: number } } | null): number
{
    const totalResultValueHereXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX = someVeryLongOptionalChainExpressionNameAAAAAAAAAAAAAAAAAAAAA?.someVeryLongPropertyNameBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB ?? anotherVeryLongDefaultExpressionNameCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC;
    return totalResultValueHereXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX;
}

function genuineTernaryStillSplitsWhenTooLong(cond: boolean): string
{
    return someConditionVariableNameHereXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
        ? someVeryLongTrueValueExpressionHereAAAAAAAAAAAAAAAAAAAAAA
        : someVeryLongFalseValueExpressionHereBBBBBBBBBBBBBBBBBBBBB;
}
