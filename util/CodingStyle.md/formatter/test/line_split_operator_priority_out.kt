/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

//%JXM_CFMT_CFG line-split-by-operator-priority=on

fun primaryTierIf(a: Boolean, b: Boolean, c: Boolean)
{
    if(someVeryLongConditionNameAAAAAAAAAAAAAA
        && anotherVeryLongConditionBBBBBBBBBBBBBBBBBBBBBB
        && yetAnotherLongConditionCCCCCCCCCCCCCCCCCCCCCC) doSomething()
}

fun elvisNeverTreatedAsTernary(x: String?): String
{
    val totalResultValueHereXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX = someVeryLongNullableExpressionNameAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA ?: anotherVeryLongDefaultExpressionNameBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB

    return totalResultValueHereXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
}
