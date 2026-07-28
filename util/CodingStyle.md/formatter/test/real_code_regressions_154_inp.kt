/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

class FooTest {
    @Test
    fun `parses correctly (no debug info)`() {
        doSomething()
    }

    @Test
    fun `handles nested (parens (deeply)) fine`() {
        val x = compute(1, 2)
        assertEquals(3, x)
    }
}
