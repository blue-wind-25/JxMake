# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT

match x:
    case Point(): print("Somewhere else")
    case _      : print("Not a point")
