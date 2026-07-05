/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package test;

public class RealCodeRegressions7 {

  void m(int flag) {
    switch (flag) {
      case 1 ->
          parseRangeSetOfSomeVeryLongName(linesBuilder, getValueFromSomewhereElse(flag, it, value));
      case 2 -> doShort();
      default -> throw new AssertionError(flag);
    }
  }

}
