/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
#pragma once

template <class T>
void from_json(T& obj) {
    [:simdjson::json_builder::expand(std::meta::nonstatic_data_members_of(^T)):] >> [&]<auto dm> {
        constexpr auto name = std::meta::identifier_of(dm);
    };
}
