/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

struct ConvD {

    int  x        = 0;
    bool gemm     = false; // 1x1, stride 1, no pad: cuBLAS gemm (cuDNN's FMA
                           // algorithms are slow here)
    bool direct_w = false; // Weight gradient by im2col

}; // struct ConvD

void step(int n)
{
    int a  = 1; // First part of a long
                // explanation, continued
                // over three lines.
    int bb = 2; // Other
    // A standalone comment at another column stays as it was
    callSomething(n, a);  // A call with
                          // a continued note
}
