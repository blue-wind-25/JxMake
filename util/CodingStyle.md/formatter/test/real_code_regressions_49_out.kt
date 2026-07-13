/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class Foo {

    fun test()
    {
        val typeSpec = TypeAliases::class.toTypeSpecWithTestHandler()

        val fooPropertyType = typeSpec.propertySpecs.first { it.name == "foo" }.type
        val fooAliasData    = fooPropertyType.tag<TypeAliasTag>()
    }

} // class Foo
