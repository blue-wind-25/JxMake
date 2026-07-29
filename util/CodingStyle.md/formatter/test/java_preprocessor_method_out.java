/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

package com.example.pp;

#define __GEN_CXI_NPR_NPR__(NAME, SUFFIX, FIMPL, FBASE) \
    public ARMCortexMThumb NAME##SUFFIX() throws JXMAsmError { return FIMPL(); }

// Regression coverage for the "preprocessor directive glued onto a following Java
// method definition" bug recorded in STATE.md's Known Gaps: a `#endif` (or any
// preprocessor line) sitting directly before a method definition inside a class
// body must not get glued onto the same output line as the method's own modifiers,
// regardless of blank lines separating them in the source.

public class Toggle {
#ifdef FEATURE_X

    private int featureFlag = 1;
#endif

    public void run()
    {
    }

#ifdef FEATURE_Y
    private int otherFlag = 2;
#endif
    public void other() throws Exception
    {
    }

    __GEN_CXI_NPR_NPR__(clrex, __NONE__, clrex_c, $clrex)
    __GEN_CXI_NPR_NPR__(dmb, _sy, dmb_sy_c, $dmb_sy)

} // class Toggle
