/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

using Foo        = int;
using Bar        = std::vector<int>;
using LongerName = std::map<std::string, int>;

template<typename T>
using Vec = std::vector<T>;

int main()
{
    using Local = double;

    return 0;
}
