# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT


def explain_template_loading_attempts(
    app: App,
    template: str,
    attempts: list[
        tuple[
            BaseLoader,
            Scaffold,
        ]
    ],
) -> None:
    pass


def f():
    if a:
        continue
    if b:
        if c:
            continue
        d()
        continue
    elif e:
        continue
