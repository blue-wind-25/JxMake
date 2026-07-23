# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT


@click.custom_version_option(lambda ctx: f"{ctx.info_name} 1.0")
def cli():
    pass
