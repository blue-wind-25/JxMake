/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class Widget {

public:
    _NODISCARD_LOCK unique_lock(
        unique_lock&& _Other
    ) noexcept : _Pmtx(
        _Other._Pmtx
    ), _Owns(_Other._Owns)
    {
    }

}; // class Widget
