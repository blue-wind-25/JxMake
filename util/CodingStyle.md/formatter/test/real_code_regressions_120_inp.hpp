/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class C {
    void current(size_type _Skip, size_type _Max_depth) {
        _TRY_BEGIN
        if (_Max_depth > _Max_frames) {
            _Max_depth = _Max_frames;
        }

        basic_stacktrace _Result{_Internal_t{}, _Max_depth, _Al};
    }
};
