#
# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT
#

def already_fits(x: int, y: int) -> int:
    return x + y


def overflow_needs_wrap(first_argument: int, second_argument: str, third_argument: "Dict[str, int]", fourth_argument: bool = False) -> None:
    ...


def type_hint_with_default_dict(config: Dict[str, int] = {}, name: str = "default", extra_long_named_argument_here: int = 0) -> None:
    ...


def returns_something(argument_number_one: int, argument_number_two: str, argument_number_three: float) -> Optional[Dict[str, int]]:
    ...


def already_wrapped(
    x    : int,
    y    : int
) -> int:
    return x + y
