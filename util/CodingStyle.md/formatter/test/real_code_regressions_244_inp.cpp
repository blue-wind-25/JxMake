/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

//%JXM_CFMT_CFG line-split-by-operator-priority=on

constexpr auto pick() -> int { return someVeryLongConditionExpressionName > anotherVeryLongThresholdName ? 1111111 : 2222222; }
