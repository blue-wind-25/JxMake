/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

#pragma once

namespace ankerl {
namespace nanobench {
namespace detail {

template <typename T>
struct PerfCountSet;

class IterationLogic;

} // namespace detail

template <typename T>
struct PerfCountSet {
    T pageFaults{};
};

} // namespace nanobench
} // namespace ankerl

namespace ankerl {
namespace nanobench {
namespace templates {

char const* json() noexcept {
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
