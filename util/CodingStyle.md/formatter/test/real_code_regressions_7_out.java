/*
 * Copyright (c) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

package test;

public class RealCodeRegressions7 {

  void m(int flag)
  {
    switch(flag) {
      case 1 ->
          parseRangeSetOfSomeVeryLongName(
              linesBuilder, getValueFromSomewhereElse(flag, it, value)
          );
      case 2  -> doShort();
      default -> throw new AssertionError(flag);
    } // switch
  }

} // class RealCodeRegressions7
