/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

/*% JXM_CFMT_CFG curly-general-scope-reindent=on; curly-general-scope-reindent-postpass=on */

package com.example.core;

// Isolates RDD_KEY_331's postMode fix for curly-general-scope-reindent-postpass. The
// else-if branch's condition is long enough to force the pipeline's own STYLE.md §8
// call-wrap; proves the postpass leaves the wrapped continuation line and its closer
// exactly as the pipeline already aligned them (the RDD_KEY_328 wrap-continuation
// over-indentation regression, fixed by this session). The surrounding one-true-brace
// `} else if (...) {` / `} else {` joined chain is the same base single-pass RDD_KEY_229
// shape as test/curly_gdr_multipass_inp.java (which resolves it via multipass instead);
// here the postpass alone (no multipass) still corrects its closing braces, since it
// re-derives brace depth from the pipeline's own already-final Allman-split structure.
public class WrapAndBraceFix {
    public String classify(int x, SomeObject someObject) {
        if (x < 0) {
            System.out.println("checking negative");
            return "neg";
        } else if( someObject.isSomethingReallyLongMethodNameThatWillDefinitelyForceLineWrap(x) == true ) {
            System.out.println("checking wrapped condition");
            return "wrapped";
        } else {
            System.out.println("checking positive");
            return "pos";
        }
    }
}

class SomeObject {
    boolean isSomethingReallyLongMethodNameThatWillDefinitelyForceLineWrap(int x) { return true; }
}
