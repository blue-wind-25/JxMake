/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

/*% JXM_CFMT_CFG indent-size=2 */

public final class RealCodeRegressions238 {

  public void m() throws Exception
  {
    for( Info info : all() ) {
      if( !isWanted( info.getName() ) ) continue;
      if( /*
 * Comment one
 */ info.getName().contains(
        "Foo"
      ) || info.getName().contains(
        "Bar"
      ) /*
 * Comment two
 */ ) continue;
      use(info);
    } // for
  }

} // class RealCodeRegressions238
