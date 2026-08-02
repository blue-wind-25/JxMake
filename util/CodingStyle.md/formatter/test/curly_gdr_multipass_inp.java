/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

/*% JXM_CFMT_CFG curly-general-scope-reindent=on; curly-general-scope-reindent-multipass=on */

package com.example.core;

// One-true-brace-style joined `} else if (...) {` / `} else {` source with
// multi-statement bodies -- the confirmed RDD_KEY_229 root-cause shape: GDR's
// pre-pass measures depth against this joined line as it exists BEFORE
// brace-placement splits it into separate `}` / `else if (...) {` lines to
// match this formatter's Allman style, so a single GDR pass alone is
// non-idempotent here (see the matching curly_gdr_multipass_inp.java entry
// in test/README.txt). Both flags are turned on via in-file config so the
// fixed 4-stage GDR/pipeline/GDR/pipeline sequence resolves it.
public class Classifier {
    public String classify(int x, int y) {
        if (x < 0) {
            System.out.println("checking negative");
            return "neg";
        } else if (x == 0) {
            System.out.println("checking zero");
            return "zero";
        } else {
            System.out.println("checking positive");
            return "pos";
        }
    }
}
