/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

/*% JXM_CFMT_CFG line-length=100;indent-size=2;indent-style=spaces;line-endings=lf;normalize-comment-start-case=on;normalize-comment-end-period=on;comment-normalization-classifier=off;closing-comment-min-lines=5;format-macros=off;header-guard-rename=off;java-import-order=java,com,org,other,local,static;java-import-sort=on;java-import-depth=2;java-import-blank-lines=1;kotlin-import-order=kotlin,java,android,com,org,other,local;kotlin-import-sort=on;kotlin-import-depth=2;kotlin-import-blank-lines=1 */

#define ENGINE_MAX_CH 8
#define ENGINE_RATE 48000

struct Engine {

  int channels;
  int rate;

}; // struct Engine
