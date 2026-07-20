#
# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT
#

from __future__ import annotations

import os
import sys

if platform.system() == "Windows":
    import winreg

import json
from . import sibling
from os import path, sep

flags    = 0x01
flags   |= 0x02
timeout  = 100
retries  = 3
# a comment breaks the group
name = "worker"

total = (something
      + something_else)

total = (
    something +
    something_else
)

squares  = [ x * x for x in range(10) ]
evens    = [ x for x in range(10) if x % 2 == 0 ]
lookup   = { k: v for k, v in items.items() }
filtered = [ y for x in data if( y := transform(x) ) is not None ]

a_slice = data[i+1:j-1]
b_slice = data[ i+1:(j*k)-1 ]
merged  = [*a, *b, *c]
config  = { **defaults, **overrides }
a_set   = { 1, 2, 3 }
a_dict  = { "a": 1, "b": 2 }

@app.route(
    "/users/<int:user_id>/orders/<int:order_id>/items/<int:item_id>/details",
    methods=["GET", "POST"],
)
def get_user_order_items(user_id: int, order_id: int, item_id: int):
    ...

@property
def x(self) -> int:
    return self._x

@x.setter
def x(self, value: int) -> None:
    self._x = value

@dataclass
class Point:
    x     : int
    y     : int
    label : str = "origin"

def process(
    extra,
    x    : int,
    y    : "List[int]",
    name : str = "default",
    desc : str = "default"
) -> Optional[str]:
    ...

def slice_params(pos, /, mid, *, kw: int = 0):
    ...

def greet(user):
    label     = f"Hello {user.first} {user.last}"
    formatted = f"{user.score:.2f}"
    raw       = f"{user.score !r}"
    nested    = f"{user.score + 1:>{width}}"

    return label

async def fetch_all(ids):
    results = [ await fetch(i) for i in ids ]

    return results

def run_command(command):
    match command.split():
        case [action]:
            run(action)
        case [action, obj]:
            run(action, obj)
        case Point(x=0, y=0):
            print("Origin")
        case Point(x=x, y=y) if x == y:
            print("Diagonal")
        case 1 | 2 | 3:
            print("small")
        case [1, 2, *rest]:
            handle(rest)
        case { "action": action, **rest }:
            handle(action, rest)
        case _:
            unknown()
    # match command.split()

def classify(code):
    match code:
        case 1: return "one"
        case 2: return "two"
        case _: return "unknown"
    # match code

def check(x):
    if x < 0:
        return None

    if x == 0:
        return 0

    return x * 2

def process_data(data):
    result = transform(data)
    validate(result)

    return result

def small(x):
    if x: return x
    while x: x -= 1

def guarded(some_long_condition_that_is_already_quite_verbose_and_overflows_the_limit_here):
    if some_long_condition_that_is_already_quite_verbose_and_overflows_the_limit_here:
        do_something()
