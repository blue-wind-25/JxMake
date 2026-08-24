/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

//%JXM_CFMT_CFG line-split-operator-priority=on

void primaryTierIf()
{
    if(someVeryLongConditionNameAAAAAAAAAAAAAA
        && anotherVeryLongConditionBBBBBBBBBBBBBBBBBBBBBB
        && yetAnotherLongConditionCCCCCCCCCCCCCCCCCCCCCC) doSomething();
}

void primaryTierWhile()
{
    while(someVeryLongConditionNameAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
        && anotherVeryLongConditionBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB) doSomething();
}

void primaryTierSwitch()
{
    switch(someVeryLongExpressionAAAAAAAAAAAAAAAAAAAA
        + anotherVeryLongExpressionBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB) {

        case 1:
            doSomething();
            break;

    } // switch
}

void primaryTierAssignmentPlus()
{
    int totalResultValueHere = someVeryLongOperandNameAAAAAAAAAAAAAAAAAAAAAAA
        + anotherVeryLongOperandNameBBBBBBBBBBBBBBBBBBBBBBBBB;
}

int primaryTierReturn()
{
    return someVeryLongOperandNameAAAAAAAAAAAAAAAAAAAAAAA
        + anotherVeryLongOperandNameBBBBBBBBBBBBBBBBBBBBBBBBB
        - yetAnotherOperandNameCCCCCCCCCCCCCCCCCCCCCCC;
}

int ternaryTierOnlyWhenStillTooLong()
{
    return someConditionVariableNameHereXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
        ? someVeryLongTrueValueExpressionHereAAAAAAAAAAAAAAAAAAAAAA
        : someVeryLongFalseValueExpressionHereBBBBBBBBBBBBBBBBBBBBB;
}

void mulDivTierOnlyWhenStillTooLong()
{
    int totalResultValueHereXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX = someVeryLongMultiplicandNameAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
        * anotherVeryLongMultiplierNameBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB;
}

void forHeaderSplit()
{
    for(
        int indexVariableNameHere = 0;
        indexVariableNameHere < someVeryLongUpperBoundVariableNameHereXXXXXXXXXXXXXXXXXX;
        ++indexVariableNameHere
    ) doSomething();
}

void forHeaderSplitWithLongBooleanCondition()
{
    for(
        int i = 0;
        someVeryLongConditionNameAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
            && anotherVeryLongConditionBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB;
        ++i
    ) doSomething();
}

void unaryOperatorsNeverMistakenForSplitPoints()
{
    int totalResultValueHereXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX = *someVeryLongPointerNameAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA;
}
