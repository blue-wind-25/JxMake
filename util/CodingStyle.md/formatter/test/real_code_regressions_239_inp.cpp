/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

//%JXM_CFMT_CFG line-split-by-operator-priority=on

void multiDeclaratorCommaList() {
    uint64_t acVeryLongResultNameHere = aVeryLongOperandName * cVeryLongOperandName, bcVeryLongResultNameHere = bVeryLongOperandName * cVeryLongOperandName;
}

void pointerTypeBeforeAngleClose() {
    if(auto* bufferPointerVariableName = dynamic_cast<SomeVeryLongNamespaceNameHere::VeryLongClassNameHere*>(sourceStreamVariableNameHere)) {
        doSomething();
    }
}
