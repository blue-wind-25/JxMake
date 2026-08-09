#
# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT
#
import sys, os

from x import (
	b,
	a,
)

import c, a

from y import (
	z,  # comment on z
	y,
)

import m, \
	l, n


def use():
	return sys.argv, os.path, b, a, c, y, z, m, l, n
