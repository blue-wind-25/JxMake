/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

package test;

#define __GEN_CXI_NPR_NPR__(NAME, SUFFIX, FIMPL, FBASE) \
    public ARMCortexMThumb NAME##SUFFIX() throws JXMAsmError { return FIMPL(); }

public class RealCodeRegressions13 {

    __GEN_CXI_NPR_NPR__( clrex, __NONE__, clrex_c, $clrex )
    __GEN_CXI_NPR_NPR__( dmb, _sy, dmb_sy_c, $dmb_sy )
}
