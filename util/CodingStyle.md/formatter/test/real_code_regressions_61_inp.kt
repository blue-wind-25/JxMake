/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
/*% JXM_CFMT_CFG indent-size=2 */
context(raise: RaiseAccumulate<Error>)
@RaiseDSL public inline fun <Error, A, B> NonEmptyList<A>.mapOrAccumulate(
  transform: context(RaiseAccumulate<Error>) (A) -> B
): NonEmptyList<B> {
  return raise.mapOrAccumulate(this, transform)
}
