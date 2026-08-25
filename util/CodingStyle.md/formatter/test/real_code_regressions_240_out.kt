/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

//%JXM_CFMT_CFG line-split-operator-priority=on

fun arraySubscriptOperatorNeverMistakenForSplitPoint(
    byteStrings                                                         : Array<IntArray>,
    i                                                                   : Int,
    byteStringOffsetXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX : Int
)
{
    if( byteStrings[i - 1][byteStringOffsetXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX] != byteStrings[i][byteStringOffsetXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX] ) doSomething()
}
