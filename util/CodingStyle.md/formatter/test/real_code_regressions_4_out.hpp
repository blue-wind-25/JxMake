/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */


#pragma once


namespace ankerl {

namespace nanobench {

namespace detail {

template<typename T>
struct PerfCountSet;

class IterationLogic;

} // namespace detail

template <typename T>
struct PerfCountSet {

    T pageFaults{};

}; // struct PerfCountSet

} // namespace nanobench

} // namespace ankerl

namespace ankerl {

namespace nanobench {

namespace templates {

char const* json() noexcept
{
    return R"DELIM({
    "results": [
{{#result}}        {
            "name": "{{name}}",
            "median(elapsed)": {{median(elapsed)}}
        }{{^-last}},{{/-last}}
{{/result}}    ]
})DELIM";
}

} // namespace templates

} // namespace nanobench

} // namespace ankerl
