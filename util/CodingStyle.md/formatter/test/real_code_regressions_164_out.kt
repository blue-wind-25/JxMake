/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class KtVisitorTest {

    fun getCode(): String
    {
        val code = """val s = "s1" + "s2" +
            |"s3"""".trimMargin()

        return code
    }

    fun fiveQuotes(): String
    {
        return """abc""""".trimMargin()
    }

} // class KtVisitorTest
