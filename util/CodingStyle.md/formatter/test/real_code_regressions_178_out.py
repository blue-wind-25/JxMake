#
# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT
#
def dispatch(event):
    match event:
        case "start":
            begin()
        # Stopping the process
        case "stop": end()
        case _:
            pass

def check(x):
    if x < 0:
        # Negative case
        return None

    # Zero case
    if x == 0: return 0
