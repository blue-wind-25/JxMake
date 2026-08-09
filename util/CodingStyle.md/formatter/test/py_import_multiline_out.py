#
# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT
#
import os, sys

from x import (
	a, b,
)

import a, c

from y import (
	z,  # Comment on z
	y,
)

import l, m, n


def use():
    return sys.argv, os.path, b, a, c, y, z, m, l, n
