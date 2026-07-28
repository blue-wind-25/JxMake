/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
public interface TypeSpecHolder {

    public val typeSpecs : List<TypeSpec>

  public interface Builder<out T : Builder<T>> {

    public fun addType(typeSpec: TypeSpec): T

    @Suppress("UNCHECKED_CAST")
    public fun addTypes(
        typeSpecs: Iterable<TypeSpec>
    ): T = apply { for(typeSpec in typeSpecs) addType(
        typeSpec
    ) } as T

    } // interface Builder

} // interface TypeSpecHolder
