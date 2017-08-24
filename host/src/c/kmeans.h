/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
/*   File:         kmeans.h   (an OpenMP version)                            */
/*   Description:  header file for a simple k-means clustering program       */
/*                                                                           */
/*   Author:  Wei-keng Liao                                                  */
/*            ECE Department Northwestern University                         */
/*            email: wkliao@ece.northwestern.edu                             */
/*   Copyright, 2005, Wei-keng Liao                                          */
/*                                                                           */
/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

// Copyright (c) 2005 Wei-keng Liao
// Copyright (c) 2011 Serban Giuroiu
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

// -----------------------------------------------------------------------------

#ifndef _H_KMEANS
#define _H_KMEANS

#include <assert.h>
#include <jni.h>
#include <immintrin.h>

#include "kmvector.h"


#define msg(format, ...) do { fprintf(stderr, format, ##__VA_ARGS__); } while (0)
#define err(format, ...) do { fprintf(stderr, format, ##__VA_ARGS__); exit(1); } while (0)

#define malloc2D(name, xDim, yDim, type) do {               \
    name = (type **)malloc(xDim * sizeof(type *));          \
    assert(name != NULL);                                   \
    name[0] = (type *)malloc(xDim * yDim * sizeof(type));   \
    assert(name[0] != NULL);                                \
    for (size_t i = 1; i < xDim; i++)                       \
        name[i] = name[i-1] + yDim;                         \
} while (0)

#ifdef __CUDACC__
inline void checkCuda(cudaError_t e) {
    if (e != cudaSuccess) {
        // cudaGetErrorString() isn't always very helpful. Look up the error
        // number in the cudaError enum in driver_types.h in the CUDA includes
        // directory for a better explanation.
        err("CUDA Error %d: %s\n", e, cudaGetErrorString(e));
    }
}

inline void checkLastCudaError() {
    checkCuda(cudaGetLastError());
}
#endif

float** omp_kmeans(int, float**, int, int, int, float, int*);
float** seq_kmeans(float**, int, int, int, float, int*, int*);
float** cuda_kmeans(float**, int, int, int, float, int*, int*);
float** reckless_kmeans(KMVector_Array*, int, int, int, float, int*, int*);
float** jni_kmeans(JNIEnv*, jobjectArray, jfieldID, int, int, int, float, int*, int*);

float** file_read(int, char*, int*, int*);
int     file_write(char*, int, int, int, float**, int*);


double  wtime(void);

// Intrinsics can be faster compiled than manual
#define USE_AVX

static inline float euclidean_baseline_float(const int n, const float* x, const float* y){
  float result = 0.f;
  for(int i = 0; i < n; ++i){
    const float num = x[i] - y[i];
    result += num * num;
  }
  return result;
}

static inline float euclidean_intrinsic_float(int n, const float* x, const float* y){
  float result=0;
  __m256 euclidean = _mm256_setzero_ps();
  //__m128 euclidean = _mm_setzero_ps();
  for (; n>3; n-=4) {
    const __m256 a = _mm256_loadu_ps(x);
    const __m256 b = _mm256_loadu_ps(y);
    const __m256 a_minus_b = _mm256_sub_ps(a,b);
    const __m256 a_minus_b_sq = _mm256_mul_ps(a_minus_b, a_minus_b);
    euclidean = _mm256_add_ps(euclidean, a_minus_b_sq);
    x+=4;
    y+=4;
  }
  const __m256 zero = _mm256_setzero_ps();
  const __m256 sum = _mm256_hadd_ps(euclidean, zero);
  // with SSE3, we could use hadd_ps, but the difference is negligible

  _mm_store_ss(&result, _mm256_castps256_ps128(sum));
  //    _mm_empty();
  if (n)
    result += euclidean_baseline_float(n, x, y);  // remaining 1-3 entries
  return result;
}


extern int _debug;

#endif
