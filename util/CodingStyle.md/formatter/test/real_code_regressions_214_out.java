/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

/*% JXM_CFMT_CFG curly-general-scope-reindent=on; curly-general-scope-reindent-multipass=on */

package com.example.core;

// Minimal repro extracted from apache/ant's JikesOutputParser.java
// (parseStandardOutput): an if/else-if/else chain whose final `} else {` is
// internally-inconsistently over-indented relative to its sibling `if`/
// `else if` in the original hand-written source. A single GDR pass alone is
// non-idempotent on this shape; both curly-general-scope-reindent=on AND
// curly-general-scope-reindent-multipass=on together resolve it -- see
// STATE_C_CPP_JAVA.md's "Known Gaps" apache/ant entry (RDD_KEY_299).
public class LineClassifier {

    private void classify(String lower, boolean emacsMode)
    {
        if( lower.contains("error") ) {
            setError(true);
        }
        else if( lower.contains("warning") ) {
            setError(false);
        }
        else {
            if(emacsMode) setError(true);
        }
    }

    private void setError(boolean err)
    {
    }

} // class LineClassifier
