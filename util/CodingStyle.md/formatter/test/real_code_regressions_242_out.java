/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

//%JXM_CFMT_CFG line-split-operator-priority=on;indent-size=2

package test;

public class RealCodeRegressions242 {

    static class Inner {

        Object keyStrength;
        Object ticker;
        Object loader;

        Inner(Object ticker, Object loader)
        {
            this.keyStrength = keyStrength;
            this.ticker      = ( ticker == Ticker.systemTicker()
              || ticker == NULL_TICKER ) ? null : ticker;
            this.loader      = loader;
        }

    } // class Inner

} // class RealCodeRegressions242
