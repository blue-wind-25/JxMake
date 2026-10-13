/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

namespace cv { namespace cuda { namespace device
{
    int x;
}}} // namespace cv { namespace cuda { namespace device

static float pick(int k)
{
    switch(k) {
        case 1: {
            float w = u(0.55f, 0.75f), h = u(0.85f, 1.0f), t = u(0.07f, 0.12f), cy = u(-0.15f, 0.15f);
            return w + h + t + cy;
        }
    }
    return 0.0f;
}
