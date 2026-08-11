#
# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT
#

@app.route(
    "/users/<int:user_id>/orders/<int:order_id>/items/<int:item_id>/details",
    methods=["GET","POST"],
)
def get_user_order_items(user_id: int, order_id: int, item_id: int):
    ...

@short.route("/x")
def short_view():
    ...

@dataclass
class Point:
    x: int
    y: int

@register()
def bare_call():
    ...

@register(
    f'Struct331_{signedness}{n}_extra_long_suffix_to_push_this_line_well_past_the_limit',
    set_name=True,
)
def make_struct(signedness, n):
    ...

@app.route(
    "/callback",
    key=lambda ctx: f"{ctx.info_name} default value padding to overflow the limit 1.0",
)
def callback_view(ctx):
    ...

@app.route( "/short", methods=["GET"] )  # Trailing comment keeps this one untouched even if long enough padding
def commented_view():
    ...
