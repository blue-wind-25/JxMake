/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
fun install()
{
    try { if(X_TYPED_ARRAYS_LONG_NAME in this) arguments.setUsingReflectionMethodLonger(
        "typedArraysLongArgumentNameHere", get(X_TYPED_ARRAYS_LONG_NAME)
    ) }
catch(e: NoSuchMethodError) { throw IllegalStateException("boom") }
}
