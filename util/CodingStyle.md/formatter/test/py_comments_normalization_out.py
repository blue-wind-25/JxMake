#
# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT
#
#% JXM_CFMT_CFG comment-normalization-classifier=off

# This is a chain comment
# it should only capitalize the first line
# and strip the sole trailing period from the last line
def greet():
    return "hi"


# noqa: this leading directive word must stay lowercase
def helper():
    # type: ignore -- this leading directive word must stay lowercase too
    return 1


x = 1  # This trailing comment is its own singleton group


def check(y):
    if y < 0:
        # Negative case
        return None

    return y
