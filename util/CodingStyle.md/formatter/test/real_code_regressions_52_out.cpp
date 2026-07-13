/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

template <class Test, class TArg>
test(
    std::string_view, std::string_view, std::string_view,
    reflection::source_location, TArg, Test
) -> test<Test, TArg>;
template <class TSuite>
struct suite {

  TSuite run{};

}; // struct suite
