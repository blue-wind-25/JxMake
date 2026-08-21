/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

/*% JXM_CFMT_CFG curly-general-scope-reindent=on;curly-general-scope-reindent-postpass=on */

/**
 * Marks a member as compile-time constant, e.g. /*static*/ in the generated output.
 * See the language reference for details.
 */
class Widget {

    fun activate()
    {
        val ready = true
        if(ready) println("activated")
    }

} // class Widget
