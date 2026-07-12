/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

#pragma once

template <int _OnCompletion>
struct __on_disp {
  template <class _Action>
  static void __impl(_Action __action) {
    int __d = get_value();
    if (__d == _OnCompletion) do_call(__action);
  }
};
