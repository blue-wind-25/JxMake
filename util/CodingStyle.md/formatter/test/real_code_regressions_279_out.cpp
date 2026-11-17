/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

namespace {

// out[j] += sum over rows (in order) of part[r][j]; one thread per column
void k_out(
    const float* a,
    const float* lum,
    float*       img,
    int          n,
    int          hw,
    int          levels,
    int          edges,
    int          z
)
{
    float* t0 = img + n;
    t0[o]             = a[0];
    t0[2 * plane + o] = lum[0];

    dz[ (size_t)s * T + j ] = 1.0f;
}

}  // namespace

class FaceCuda {

    std::vector<int> nodes_;
    std::vector<size_t> off_, len_, bn_off_;
    size_t total_ = 0, bn_total_ = 0;
    std::string err_, describe_;
    float* d_img_ = nullptr; // d_img_: this step's images

}; // class FaceCuda

void run(int hw, int n)
{
    int count = 0; // y += x
    const dim3 blk(16, 16, 1), grd( (hw + 15) / 16, n );
    std::map<int, int> m;
}
