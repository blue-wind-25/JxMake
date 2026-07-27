/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
const isAccessor = (declaration): declaration is AccessorDeclaration | PropertyDeclaration => true;

const isBlockLike = (block): block is Block | SourceFile | Program => true;

const pick = (x): AccessorDeclaration | PropertyDeclaration => x;
