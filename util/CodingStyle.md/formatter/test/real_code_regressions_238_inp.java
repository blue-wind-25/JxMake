/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

/*% JXM_CFMT_CFG indent-size=2 */

public final class RealCodeRegressions238 {
  public void m() throws Exception {
    for (Info info : all()) {
      if (!isWanted(info.getName())) {
        continue;
      }
      if (
      /*
       * comment one
       */
      info.getName().contains("Foo")
          || info.getName().contains("Bar")
      /*
       * comment two
       */
      ) {
        continue;
      }
      use(info);
    }
  }
}
