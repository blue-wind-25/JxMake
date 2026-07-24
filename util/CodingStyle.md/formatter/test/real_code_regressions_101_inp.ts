/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
type Node = any;
export const isFunctionType = (node: Node): node is Function => {
  return true;
};

const double = x => x * 2;
