/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
/*% JXM_CFMT_CFG indent-size=2 */
fun foo(a: Int, b: Int, c: Int, d: Int): List<Int>
{
  return if(a > 0 && b > 0 && c > 0 && d > 0) listOf(a, b, c, d)
  else buildList(10) {
    if(a > 0) add(a)
    if(b > 0) add(b)
    if(c > 0) add(c)
    if(d > 0) add(d)
  }
}
