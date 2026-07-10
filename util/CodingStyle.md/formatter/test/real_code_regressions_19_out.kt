/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Found via dogfood-testing against RobotCoding gui_frontend_android's MainActivity.kt: a
// Closing-brace indentation drift bug in ScopePipeline.processScope, exercised by
// `_checkRecovery()`'s AlertDialog.Builder chain. A trailing lambda argument's `{` can sit on a
// Continuation line of a multi-line fluent chain (`.setPositiveButton("Ok") {`), deeper than the
// Chain's own first line (`AlertDialog.Builder(this)`). processScope derived the lambda body's
// Indent, and its closing `}`'s alignment, from the whole STATEMENT's first line (via
// FindParentIndent, needed elsewhere for e.g. `case 1:` labels) rather than the brace's own
// Physical line -- under-indenting the body by one level and misplacing the nested if/else
// Block's closing braces to match.
class Repro {

    private fun checkRecovery()
    {
        if( !hasRecovery(this) ) return
        AlertDialog.Builder(this)
            .setTitle("title")
            .setPositiveButton("Ok") { _, _ ->
                val blocks = loadRecovery(this)
                if(blocks != null) {
                    vm.loadBlocks(blocks)
                }
                else {
                    deleteRecovery(this)
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                deleteRecovery(this)
            }
            .setCancelable(false)
            .show()
    }

} // class Repro
