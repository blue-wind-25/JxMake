/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

struct ConvD {
    int x = 0;
    bool gemm = false;    // 1x1, stride 1, no pad: cuBLAS gemm (cuDNN's FMA
                          // algorithms are slow here)
    bool direct_w = false;  // weight gradient by im2col
};

void step(int n)
{
    int a = 1;    // first part of a long
                  // explanation, continued
                  // over three lines.
    int bb = 2;   // other
    // a standalone comment at another column stays as it was
    callSomething(n, a);  // a call with
                          // a continued note
}
