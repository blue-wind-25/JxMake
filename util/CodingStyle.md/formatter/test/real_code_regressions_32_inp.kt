/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
internal fun get(type: Type): TypeName {
  return when (type) {
    is Class<*> ->
      when {
        type === Void.TYPE -> UNIT
        else -> type.asClassName()
      }
    is ParameterizedType -> ParameterizedTypeName.get(type)
    else -> throw IllegalArgumentException("unexpected type: $type")
  }
}
