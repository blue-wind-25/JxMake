/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

void spacing(int r, int c, int x, bool valid, Foo&& a)
{
    size_t n = sizeof(float) * x;
    int    d = r & ~c;
    c = r & ~c;
    bool v = check(x) && valid;
    valid = check(x) && valid;
    bool   z    = (r && c) || (x && *ptr);
    auto&& keep = get();
    auto f = [](Foo&& b, int k) { return k; };
    auto g = static_cast < Foo&& > (a);
}

Holder::~Holder() {}
