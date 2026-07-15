/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
/*% JXM_CFMT_CFG indent-size=2 */
context(
  raise : Raise<Error>
) @RaiseDSL @IgnorableReturnValue public inline fun <Error, B : Any> ensureNotNull(
  value     : B?,
  otherwise : () -> Error
): B
{
  return raise.ensureNotNullExt(value, otherwise)
}

public inline fun <T, A, B> Iterable<T>.separateEither( f: (T) -> Either<A, B> ): Pair<List<A>, List<B>>
{
  val left  = mutableListOf<A>()
  val right = mutableListOf<B>()

  for(item in this) {
    when( val either = f(item) ) {

      is Left  -> left.add(either.value)
      is Right -> right.add(either.value)

    } // when val either = f(item)
  } // for

  return Pair(left, right)
}
