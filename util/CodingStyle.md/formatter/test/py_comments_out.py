#
# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT
#

# Module setup
import os
import sys
# local helper
from . import sibling

flags  = 0x01
flags |= 0x02
# comment breaks the group
timeout = 100

# Build the lookup table
lookup   = {k:v for k,v in items.items()} # inline note
filtered = [y for x in data if (y := transform(x)) is not None] # keep truthy values

@dataclass
class Point:
    x: int  # horizontal position
    # vertical position
    y: int

def process(extra, x: int,  # required
y: "List[int]") -> Optional[str]:
    ...

@app.route("/status") # health check endpoint
def status():
    """
    Health check endpoint.
        Always returns "ok" for now.
    """

    return "ok"

def dispatch(event):
    match event:
        case "start":
            begin()
        # stopping the process
        case "stop": end()
        case _:
            pass

def check(x):
    if x < 0:
        # negative case
        return None

    # zero case
    if x == 0:
        return 0

    return x * 2

def classify(code):
    match code:
        case 1: return "one" # first
        case 2: return "two"
        # fallback
        case _: return "unknown"
