#
# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT
#

def f(
    number        : int,
    no_annotation = None,
    text          : str  = "default",
    *            ,
    debug         : bool = False,
    **kwargs
) -> str:
    ...
