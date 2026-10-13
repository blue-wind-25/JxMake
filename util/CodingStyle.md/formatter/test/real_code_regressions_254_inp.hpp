/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

template<typename _Tp> inline _Tp cv_abs(_Tp x) { return std::abs(x); }
inline int cv_abs(uchar x) { return x; }
inline int cv_abs(schar x) { return std::abs(x); }
inline int cv_abs(ushort x) { return x; }
