/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class recursive_directory_iterator {

public:
    recursive_directory_iterator() noexcept = default;
    recursive_directory_iterator(
        const recursive_directory_iterator&
    ) noexcept = default; // Strengthened
    recursive_directory_iterator(recursive_directory_iterator&&) noexcept = default;
    ~recursive_directory_iterator() noexcept                              = default;

}; // class recursive_directory_iterator
