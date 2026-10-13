/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

/*% JXM_CFMT_CFG --lang=c */

#define SQUARE(x) ((x) * (x))

static const int kScale = 20 * 60;
static int sq(int v) { return SQUARE(v) * 2; }
