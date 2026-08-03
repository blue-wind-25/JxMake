/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Regression: a single-declarator `const`/`let` with a type annotation and no
// alignment-group neighbors rendered with a stray space before the colon
// (`const x : number = 1;`) because JsTsDeclarationAlignmentRule.renderAlignedGroup
// always put the `: type` text in its own ColumnGrid cell, and ColumnGrid always
// joins adjacent cells with a single space -- even for a one-row "group". Fixed by
// merging the name and `: type` text into one cell whenever group.size() == 1.
const x: number = 1;

let y: string;

function f() {
    const a       = 1;
    const bb: string = "s";
    const ccc     : number = 3;
}
