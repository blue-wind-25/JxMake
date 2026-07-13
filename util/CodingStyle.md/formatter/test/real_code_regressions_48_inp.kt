/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class Container {
    private val Class<*>.descriptor: String
        get() {
            return when {
                isPrimitive -> when (kotlin) {
                    Byte::class -> "B"
                    Char::class -> "C"
                    else -> "?"
                }
                isArray -> name.replace('.', '/')
                else -> "L$name;".replace('.', '/')
            }
        }

    fun toCodeBlock(value: Int): CodeBlock {
        return when {
            value == 1 -> CodeBlock.of("%L", value)
            value == 2 ->
                buildCodeBlock {
                    add("%T::class", ARRAY)
                    add(">")
                }
            else -> CodeBlock.of("%L", value)
        }
    }
}
