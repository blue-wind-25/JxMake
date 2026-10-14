/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

class PostfixIncrementSpacing {

    int fill(int[] vals, int[] src, int n)
    {
        for(int y = 0; y < 4; ++y) {
            if(y < 2) vals[n++] = src[y];
        }
        vals[n++] = 1;
        vals[n--] = src[n];

        return n;
    }

} // class PostfixIncrementSpacing
