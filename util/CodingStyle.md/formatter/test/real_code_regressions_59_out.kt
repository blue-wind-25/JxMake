/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
/*% JXM_CFMT_CFG indent-size=2 */
public operator fun <A : Comparable<A>, B : Comparable<B>> Pair<A, B>.compareTo(other: Pair<A, B>): Int
{
  val first = first.compareTo(other.first)

  return if(first == 0) second.compareTo(other.second)
  else first
}
