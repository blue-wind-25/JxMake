/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
template <class _Vw>
class chunk_view {
private:
    /* [[no_unique_address]] */ _Vw _Range;
    range_difference_t<_Vw> _Count;
    range_difference_t<_Vw> _Remainder = 0;
};
