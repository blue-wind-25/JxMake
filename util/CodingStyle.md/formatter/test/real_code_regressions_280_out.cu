/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

           __constant__ float kScale[4];
__device__              int   g_count;

static __device__ __forceinline__ float sq(float v) { return v * v; }

__host__ __device__ inline float twice(float v) { return 2.0f * v; }

__global__ void k_sum(const float* x, float* out, int n)
{
    __shared__       float sh[256];
               const int   i   = blockIdx.x * blockDim.x + threadIdx.x;
                     float acc = 0.0f;
    if(i < n) acc = sq( x[i] ) * kScale[0];
    sh[threadIdx.x] = acc;
    __syncthreads();
    if(threadIdx.x == 0) out[blockIdx.x] = twice( sh[0] );
}

void launch(const float* x, float* out, int n, cudaStream_t s)
{
    k_sum<<<(n + 255) / 256, 256, 0, s>>>(x, out, n);
}
