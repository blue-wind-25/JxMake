/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

void templates(Foo&& a, int x, int y, int w, int z)
{
    auto                         g   = std::forward<T&&>(a);
    std::function<void(Foo&& b)> h1  = h;
    std::function<int(int, int)> add = h;
    std::vector<Foo&&>           v;
    g = static_cast<Foo&&>(a);
    bool c1 = x < y && w > z;
    bool c2 = x < y && w > (z);
}
