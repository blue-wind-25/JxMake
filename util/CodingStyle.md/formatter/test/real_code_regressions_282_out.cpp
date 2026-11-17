/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

void f(double value)
{
  {
      glz::patch_document ops = { { glz::patch_op_type::add, "/b", glz::generic(
          value
      ), std::nullopt } };
  }
}

void g(float* t0, int plane, int o, int s, int T)
{
    t0[o]                   = 1;
    t0[2 * plane + o]       = 2;
    t0[ (size_t)s * T + o ] = 3;
    t0[s * T + o]           = 4;
}
