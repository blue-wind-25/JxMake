/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

struct Engine
#if 1
    : Base
#endif
{
    int channels;
};

namespace ranges
{
struct Widget
#ifdef WIDGET_EXTRA
    : Extra
#endif
{
    int rate;
};
} // namespace ranges
