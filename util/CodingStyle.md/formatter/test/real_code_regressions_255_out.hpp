/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

namespace a {

    //! @name Wide LUT of elements
    //! @{
    //! @brief Load maximum available capacity register contents with array elements by provided indexes
    inline v_uint8 vx_lut(const uchar* ptr, const int* idx) { return VXPREFIX(_lut)(ptr, idx); }
    inline v_int8 vx_lut(const schar* ptr, const int* idx) { return VXPREFIX(_lut)(ptr, idx); }
    inline v_uint16 vx_lut(const ushort* ptr, const int* idx) { return VXPREFIX(_lut)(ptr, idx); }
    inline v_int16 vx_lut(const short* ptr, const int* idx) { return VXPREFIX(_lut)(ptr, idx); }
    inline v_int32 vx_lut(const int* ptr, const int* idx) { return VXPREFIX(_lut)(ptr, idx); }
    inline v_uint32 vx_lut(const unsigned* ptr, const int* idx) { return VXPREFIX(_lut)(ptr, idx); }
    inline v_float32 vx_lut(const float* ptr, const int* idx) { return VXPREFIX(_lut)(ptr, idx); }
    inline v_int64 vx_lut(const int64* ptr, const int* idx) { return VXPREFIX(_lut)(ptr, idx); }
    inline v_uint64 vx_lut(const uint64* ptr, const int* idx) { return VXPREFIX(_lut)(ptr, idx); }
#if CV_SIMD_64F
    inline v_float64 vx_lut(const double* ptr, const int* idx) { return VXPREFIX(_lut)(ptr, idx); }

#endif
} // namespace a
