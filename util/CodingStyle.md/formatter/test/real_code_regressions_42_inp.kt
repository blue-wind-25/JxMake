/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
public open class ReallyLongHeapClassName<T> : SomeBaseClassWithALongName() where T: SomeNodeInterface, T: Comparable<T> {
    private var a: Array<T?>? = null

    private fun swap(i: Int, j: Int) {
        val tmp = a!![i]
        a!![i] = a!![j]
        a!![j] = tmp
    }
} // class ReallyLongHeapClassName
