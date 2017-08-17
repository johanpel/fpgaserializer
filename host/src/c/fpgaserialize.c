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

//#define DEBUG

#ifdef DEBUG
#define DEBUG_PRINT(...) do{ fprintf( stdout, __VA_ARGS__ ); fflush(stdout);} while( 0 )
#else
#define DEBUG_PRINT(...) do{ } while ( 0 )
#endif

int _debug = 0;

JavaVM * global_vm = NULL;

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
  global_vm = vm;
  return JNI_VERSION_1_2;
}

static inline void start_time(struct timespec * start) {
  clock_gettime(CLOCK_MONOTONIC, start); /* mark start time */
}

static inline uint64_t diff_time(struct timespec start) {
  uint64_t diff;
  struct timespec end;

  clock_gettime(CLOCK_MONOTONIC, &end); /* mark the end time */
  diff = BILLION * (end.tv_sec - start.tv_sec) + end.tv_nsec - start.tv_nsec;
  return diff;
}

static inline uint64_t stop_time(struct timespec * start) {
  uint64_t diff;
  struct timespec end;

  clock_gettime(CLOCK_MONOTONIC, &end); /* mark the end time */
  diff = BILLION * (end.tv_sec - start->tv_sec) + end.tv_nsec - start->tv_nsec;
  DEBUG_PRINT("%8llu, ", (long long unsigned int) diff);
  fflush(stdout);
  return diff;
}

void print_output(int * membership, int objects)
{
  for (int i = 0; i < objects; i++) {
    DEBUG_PRINT("%d, ", membership[i]);
  }
  fflush(stdout);
}

void print_input(float ** vecs, int size, int dims) {
  for (int i = 0; i < size; i++) {
    DEBUG_PRINT("%d=[", i);
    for (int j = 0; j < dims; j++) {
      DEBUG_PRINT("%.1f",vecs[i][j]);
      if (j!=dims-1)
        DEBUG_PRINT(",");
    }
    DEBUG_PRINT("] ");
  }
}

JNIEXPORT jlong JNICALL Java_org_tudelft_ewi_ce_fpgaserialize_fpgaserialize_00024_testKMeansJNI(
    JNIEnv * env,
    jobject me,
    jobjectArray in,
    jint dims,
    jint centers,
    jint mode,
    jarray out)
{
  struct timespec timer;

  jclass klass = NULL;
  jfieldID valuesFID = NULL;
  jsize objCnt = NULL;

  klass = (*env)->FindClass(env, "org/tudelft/ewi/ce/fpgaserialize/KMVector");
  valuesFID = (*env)->GetFieldID(env, klass, "values", "[F");
  objCnt = (*env)->GetArrayLength(env, in);

  //print_input(cVecs, objCnt, dims);

  start_time(&timer);

  int* membership = (int*) malloc(objCnt * sizeof(int));
  int iters;
  if (mode == 1)
    jni_kmeans(env, in, valuesFID, dims, objCnt, centers, 0.001f, membership, &iters);

  return stop_time(&timer);

  //print_output(membership, objCnt);
  //free membership
}

JNIEXPORT jlong JNICALL Java_org_tudelft_ewi_ce_fpgaserialize_fpgaserialize_00024_testKMeansJNISerialized(
    JNIEnv * env,
    jobject me,
    jobjectArray in,
    jint dims,
    jint centers,
    jint mode,
    jarray out)
{
  struct timespec timer;

  //start_time(&timer);

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

    obj = (*env)->GetObjectArrayElement(env, in, i);
    vals = (jarray)(*env)->GetObjectField(env, obj, valuesFID);
    float* coords = (float*)(*env)->GetPrimitiveArrayCritical(env, vals, NULL);
    assert(coords != NULL);
    memcpy(cVecs[i], coords, dims * sizeof(float));
    (*env)->ReleasePrimitiveArrayCritical(env, vals, coords, JNI_ABORT);
  }

  //stop_time(&timer);

  //print_input(cVecs, objCnt, dims);

  start_time(&timer);

  int* membership = (int*) malloc(objCnt * sizeof(int));
  int iters;
  if (mode == 1)
    seq_kmeans(cVecs, dims, objCnt, centers, 0.001f, membership, &iters);
  //if (mode == 2)
    //omp_kmeans(1, cVecs, dims, objCnt, centers, 0.001f, membership);
  //if (mode == 3)
    //cuda_kmeans(cVecs, dims, objCnt, centers, 0.001f, membership, &iters);

  return stop_time(&timer);

  //print_output(membership, objCnt);
  //free membership
}


JNIEXPORT jlong JNICALL Java_org_tudelft_ewi_ce_fpgaserialize_fpgaserialize_00024_testKMeansUnsafe(
    JNIEnv * env,
    jobject me,
    jlong in,
    jint objCnt,
    jint dims,
    jint centers,
    jint mode,
    jarray out)
{
  struct timespec timer;

  //start_time(&timer);

  int i;
  float** cVecs = (float**) malloc(objCnt * sizeof(float*));
  cVecs[0] = (float*) in;
  for (i=1; i<objCnt; i++)
    cVecs[i] = cVecs[i-1] + dims;

  //stop_time(&timer);

  //print_input(cVecs, objCnt, dims);

  start_time(&timer);

  int* membership = (int*) malloc(objCnt * sizeof(int));
  int iters;

  if (mode == 1)
    seq_kmeans(cVecs, dims, objCnt, centers, 0.001f, membership, &iters);
  //if (mode == 2)
    //omp_kmeans(1, cVecs, dims, objCnt, centers, 0.001f, membership);
  //if (mode == 3)
    //cuda_kmeans(cVecs, dims, objCnt, centers, 0.001f, membership, &iters);

  return stop_time(&timer);

  //print_output(membership, objCnt);
  //free membership
}

JNIEXPORT jlong JNICALL Java_org_tudelft_ewi_ce_fpgaserialize_fpgaserialize_00024_testKMeansRecklessSerialized(
    JNIEnv * env,
    jobject me,
    jobjectArray in,
    jint dims,
    jint centers,
    jint mode,
    jarray out)
{
  struct timespec timer;

  int i;

  //start_time(&timer);

  void * vecArrayOop = resolveJNIHandle(in);
  KMVector_Array vecArray = get_KMVector_Array(vecArrayOop);

  float** cVecs = (float**) malloc(vecArray._size * sizeof(float*));
  cVecs[0] = (float*)  malloc(vecArray._size * dims * sizeof(float));
  for (i=1; i<vecArray._size; i++)
    cVecs[i] = cVecs[i-1] + dims;

  for (i=0;i < vecArray._size; i++) {
    KMVector vec = get_KMVector(vecArray._values[i]);
    memcpy(cVecs[i], vec.values._values, vec.values._size * sizeof(float));
  }

  //stop_time(&timer);

  //print_input(cVecs, vecArray.size, dims);

  start_time(&timer);
  int* membership = (int*) malloc(vecArray._size * sizeof(int));
  int iters;
  if (mode == 1)
    seq_kmeans(cVecs, dims, vecArray._size, centers, 0.001f, membership, &iters);
  //if (mode == 2)
    //omp_kmeans(1, cVecs, dims, vecArray.size, centers, 0.001f, membership);
  //if (mode == 3)
    //cuda_kmeans(cVecs, dims, vecArray.size, centers, 0.001f, membership, &iters);
  return stop_time(&timer);

  //print_output(membership, vecArray._size);
  //free membership
}

JNIEXPORT jlong JNICALL Java_org_tudelft_ewi_ce_fpgaserialize_fpgaserialize_00024_testKMeansReckless(
    JNIEnv * env,
    jobject me,
    jobjectArray in,
    jint dims,
    jint centers,
    jint mode,
    jarray out)
{
  struct timespec timer;

  //start_time(&timer);

  void * vecArrayOop = resolveJNIHandle(in);
  KMVector_Array vecArray = get_KMVector_Array(vecArrayOop);

  //stop_time(&timer);

  //print_input(cVecs, vecArray.size, dims);

  start_time(&timer);

  int* membership = (int*) malloc(vecArray._size * sizeof(int));
  int iters;
  if (mode == 1)
    reckless_kmeans(&vecArray, dims, vecArray._size, centers, 0.001f, membership, &iters);
  //if (mode == 2)
    //omp_kmeans(1, cVecs, dims, vecArray.size, centers, 0.001f, membership);
  //if (mode == 3)
    //cuda_kmeans(cVecs, dims, vecArray.size, centers, 0.001f, membership, &iters);
  return stop_time(&timer);

  //print_output(membership, vecArray._size);
  //free membership
}
