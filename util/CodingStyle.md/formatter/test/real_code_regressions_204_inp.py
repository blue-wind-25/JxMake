"""
Copyright (C) 2024 Example Corp.
SPDX-License-Identifier: MIT
"""

import sys


def warn_missing(orig_exists, fmt_exists, orig_arg, fmt_arg):
    if not orig_exists or not fmt_exists:
        if not orig_exists and not fmt_exists: sys.stderr.write("WARNING: both %s and %s are missing\n" % (orig_arg, fmt_arg))
        elif not orig_exists:                  sys.stderr.write("WARNING: %s is missing\n" % orig_arg)
        else:                                  sys.stderr.write("WARNING: %s is missing\n" % fmt_arg)
        sys.exit(1)
