/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
void f()
{
  {
      glz::patch_document ops = { {glz::patch_op_type::add, "/b", glz::generic(
          2.0
      ), std::nullopt} };
  }
}
