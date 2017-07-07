#if __STDC_VERSION__ >= 199901L
#define _XOPEN_SOURCE 600
#else
#define _XOPEN_SOURCE 500
#endif /* __STDC_VERSION__ */
#include <time.h>

#include <jni.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <omp.h>
#include <string.h>

#include "fpgaserialize.h"
#include "kmvector.h"
#include "kmeans.h"

#define BILLION 1000000000L
#define START_TIME_MEASURE clock_gettime(CLOCK_MONOTONIC, &start);

//#define REPEAT
int repeats = 1024;

int _debug = 0;

struct timespec start, end;

JavaVM * global_vm = NULL;

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
  global_vm = vm;
  return JNI_VERSION_1_2;
}

static inline void start_time() {
  clock_gettime(CLOCK_MONOTONIC, &start); /* mark start time */
}

static inline uint64_t diff_time() {
  uint64_t diff;
  clock_gettime(CLOCK_MONOTONIC, &end); /* mark the end time */
  diff = BILLION * (end.tv_sec - start.tv_sec) + end.tv_nsec - start.tv_nsec;
  return diff;
}

static inline void stop_time() {
  uint64_t diff;
  clock_gettime(CLOCK_MONOTONIC, &end); /* mark the end time */
  diff = BILLION * (end.tv_sec - start.tv_sec) + end.tv_nsec - start.tv_nsec;
  printf("%llu, ", (long long unsigned int) diff);
  fflush(stdout);
}

JNIEXPORT void JNICALL Java_org_tudelft_ewi_ce_fpgaserialize_fpgaserialize_00024_testKMeans(
    JNIEnv * env,
    jobject me,
    jobjectArray in,
    jint dims,
    jint centers,
    jint mode,
    jarray out)
{
  int i;

  //start_time();

  void * vecArrayOop = resolveJNIHandle(in);
  KMVector_Array vecArray = get_KMVector_Array(vecArrayOop);

  float** cVecs = (float**) malloc(vecArray.size * sizeof(float*));
  cVecs[0] = (float*)  malloc(vecArray.size * dims * sizeof(float));
  for (i=1; i<vecArray.size; i++)
    cVecs[i] = cVecs[i-1] + dims;

  //stop_time();
  //start_time();

  for (i=0;i < vecArray.size; i++) {
    KMVector vec = get_KMVector(vecArray.values[i]);
    memcpy(cVecs[i], vec.values.values, vec.values.size * sizeof(float));
  }

  //stop_time();
  //start_time();

  int* membership = (int*) malloc(vecArray.size * sizeof(int));
  int iters;
  if (mode == 1)
    seq_kmeans(cVecs, dims, vecArray.size, centers, 0.001f, membership, &iters);
  if (mode == 2)
    omp_kmeans(1, cVecs, dims, vecArray.size, centers, 0.001f, membership);
  //if (mode == 3)
    //cuda_kmeans(cVecs, dims, vecArray.size, centers, 0.001f, membership, &iters);
  //printf("Took %d iters\n",iters);
  //stop_time();

  fflush(stdout);
}

JNIEXPORT void JNICALL Java_org_tudelft_ewi_ce_fpgaserialize_fpgaserialize_00024_testKMeansJNI(
    JNIEnv * env,
    jobject me,
    jobjectArray in,
    jint dims,
    jint centers,
    jint mode,
    jarray out)
{

  jclass klass = NULL;
  jfieldID valuesFID = NULL;
  jsize objCnt = NULL;

  klass = (*env)->FindClass(env, "org/tudelft/ewi/ce/fpgaserialize/KMVector");
  valuesFID = (*env)->GetFieldID(env, klass, "values", "[F");
  objCnt = (*env)->GetArrayLength(env, in);


  float** cVecs = (float**) malloc(objCnt * sizeof(float*));
  cVecs[0] = (float*)  malloc(objCnt * dims * sizeof(float));
  for (int i=1; i<objCnt; i++)
    cVecs[i] = cVecs[i-1] + dims;

  for (int i=0;i < objCnt; i++) {
    jobject obj;
    jarray vals;
    start_time();
    for (int j=0; j<1000;j++)
    obj = (*env)->GetObjectArrayElement(env, in, i);
    stop_time();
    //start_time();
    for (int j=0; j<1000;j++)
    vals = (jarray)(*env)->GetObjectField(env, obj, valuesFID);
    //stop_time();
    float* coords = (float*)(*env)->GetPrimitiveArrayCritical(env, vals, NULL);
    assert(coords != NULL);
    memcpy(cVecs[i], coords, dims * sizeof(float));
    (*env)->ReleasePrimitiveArrayCritical(env, vals, coords, JNI_ABORT);
  }

  int* membership = (int*) malloc(objCnt * sizeof(int));
  int iters;
  if (mode == 1)
    seq_kmeans(cVecs, dims, objCnt, centers, 0.001f, membership, &iters);
  if (mode == 2)
    omp_kmeans(1, cVecs, dims, objCnt, centers, 0.001f, membership);
  //if (mode == 3)
    //cuda_kmeans(cVecs, dims, objCnt, centers, 0.001f, membership, &iters);
  //printf("Took %d iters\n",iters);

  fflush(stdout);
}
