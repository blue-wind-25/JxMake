/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class Container {
    internal class Parameterized<
        Simple,
        ExtendsClass : Number,
        ExtendsInterface : Runnable,
        ExtendsTypeVariable : Simple,
        Intersection,
        IntersectionOfInterfaces>
    where IntersectionOfInterfaces : Runnable, Intersection : Number, Intersection : Runnable,
          IntersectionOfInterfaces : Serializable

    fun getTypeVariableTypeMirror() {
        val x = 1
    }
}
