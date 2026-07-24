/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
function mergeProps<
  T,
  U
>(a: T, b: U): T & U
{
  return { ...a, ...b };
}

class Box<
  T,
  U
> {

    value : T;

} // class Box
