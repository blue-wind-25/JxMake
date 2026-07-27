/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class Foo {
    fun test() {
        src.copyToRecursively(dst, followLinks = false, onError = { source, target, exception ->
            assertIs<java.nio.file.NoSuchFileException>(exception)
            assertEquals(src, source)
            assertEquals(dst, target)
            OnErrorResult.SKIP_SUBTREE
        })
    }
}
