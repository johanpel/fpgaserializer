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
#include "pictures.h"
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

void printBytes(FILE * f, void * data, int bytes)
{
  unsigned char * d = (unsigned char*) data;
  for (int i=0;i<bytes;i++)
  {
    fprintf(f, "%02X ", d[i]);
  }
  fprintf(f,"\n");
}

void printHexEditorView(FILE * f, void * data, int bytes)
{
  unsigned char * d = (unsigned char*) data;

  fflush(f);

  /*fprintf(f, "  |");
  for (int i = 0; i < 8; i++)
    fprintf(f, "%02X|", i);
  fprintf(f, "\n");

  fprintf(f, "  |");
  for (int i = 0; i < 8; i++)
    fprintf(f, "--|");
  fprintf(f, "\n");
*/
  int i = 0;
  while (bytes > 0) {
    fprintf(f, "%04d => X\"", i / 8);
    for (int j = 0; (j < 8) && (bytes > 0); j++) {
      fprintf(f, "%02X", d[i]);
      i++;
      bytes--;
    }
    fprintf(f, "\",\n");
  }
  fflush(f);
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

static inline void stop_time(char * timer_name) {
  uint64_t diff;
  clock_gettime(CLOCK_MONOTONIC, &end); /* mark the end time */
  diff = BILLION * (end.tv_sec - start.tv_sec) + end.tv_nsec - start.tv_nsec;
  printf("%llu, ", (long long unsigned int) diff);
  fflush(stdout);
}

union argb {
     unsigned int i;
     unsigned char color[4];
};

static inline void convolute(unsigned int* in, unsigned int* out, int* coeffs, int scale, int K, int w, int h) {
  int KO = 0;//K / 2;
  for (int y=KO;y<h-KO;y++) {
    for (int x=KO;x<w-KO;x++) {
      union argb pixel;
      union argb result;
      //int r = 0, g = 0, b = 0, a = 0;
      float gray = 0.0f;
      /*for (int ky = 0; ky < K; ky++) {
        for (int kx=0;kx<K; kx++) {
          int coeff = coeffs[ky * K + kx];
          pixel.i = in[(y+ky-KO)*w+(x+kx-KO)];
          gray = 0.3 * pixel.color[0] + 0.6 * pixel.color[1] + 0.1 * pixel.color[3];
          r += gray * coeff;
          g += gray * coeff;
          b += gray * coeff;
        }
      }
      result.color[0] = r / scale;
      result.color[1] = g / scale;
      result.color[2] = b / scale;
      result.color[3] = 255;
      */
      pixel.i = in[y*w+x];
      gray = 0.3 * pixel.color[0] + 0.6 * pixel.color[1] + 0.1 * pixel.color[3];
      result.color[0] = gray;
      result.color[1] = gray;
      result.color[2] = gray;
      result.color[3] = 255;
      out[y*w+x] = result.i;
    }
  }
}

JNIEXPORT void JNICALL Java_org_tudelft_ewi_ce_fpgaserialize_fpgaserialize_00024_testPictures(
    JNIEnv * env,
    jobject this,
    jobject in,
    jobject out)
{
  int coeffs[25] = {
      2,   1,   0,   -1,  -2,
      3,   2,   0,   -2,  -3,
      4,   3,   0,   -3,  -4,
      3,   2,   0,   -2,  -3,
      2,   1,   0,   -1,  -2
  };
  int scale = 50;

  void* in_oop = NULL;
  void* out_oop = NULL;
  SimpleImage_Array in_imgs;
  SimpleImage_Array out_imgs;

  start_time();
  stop_time("calib");

  start_time();
#ifdef REPEAT
  for (int r=0;r<repeats;r++) {
#endif
    in_oop = resolveJNIHandle(in);
    out_oop = resolveJNIHandle(out);

    in_imgs = get_SimpleImage_Array(in_oop);
    out_imgs = get_SimpleImage_Array(out_oop);
#ifdef REPEAT
  }
#endif
  stop_time("RECKLESS Init");

  uint64_t img_init_total = 0;
  uint64_t img_conv_total = 0;

  for (int i = 0; i < in_imgs.size; i++) {
    int w=0,h=0;
    SimpleImage in_img;
    SimpleImage out_img;
    start_time();
#ifdef REPEAT
    for (int r=0;r<repeats;r++) {
#endif
      in_img = get_SimpleImage(in_imgs.values[i]);
      out_img = get_SimpleImage(out_imgs.values[i]);
      w = in_img.w;
      h = in_img.h;
#ifdef REPEAT
    }
#endif
    img_init_total += diff_time();
    start_time();
    convolute((unsigned int*)in_img.pixels.values, (unsigned int*)out_img.pixels.values, coeffs, scale, 5, w, h);
    img_conv_total += diff_time();
  }
  printf("%llu, %llu, ", (long long unsigned int)img_init_total, (long long unsigned int)img_conv_total);
  fflush(stdout);
}

JNIEXPORT void JNICALL Java_org_tudelft_ewi_ce_fpgaserialize_fpgaserialize_00024_testPicturesJNI(
    JNIEnv * env,
    jobject this,
    jobject in,
    jobject out)
{

  int coeffs[25] = {
      2,   1,   0,   -1,  -2,
      3,   2,   0,   -2,  -3,
      4,   3,   0,   -3,  -4,
      3,   2,   0,   -2,  -3,
      2,   1,   0,   -1,  -2
  };
  int scale = 50;

  jclass klass = NULL;
  jfieldID wFID = NULL;
  jfieldID hFID = NULL;
  jfieldID pixelsFID = NULL;
  jsize imageCnt = NULL;

  (*env)->FindClass(env, "org/tudelft/ewi/ce/fpgaserialize/SimpleImage");

  start_time();
  stop_time("calib");

  start_time();

#ifdef REPEAT
  for (int r=0;r<repeats;r++) {
#endif
    klass = (*env)->FindClass(env, "org/tudelft/ewi/ce/fpgaserialize/SimpleImage");

    wFID = (*env)->GetFieldID(env, klass, "w", "I");
    hFID = (*env)->GetFieldID(env, klass, "h", "I");
    pixelsFID = (*env)->GetFieldID(env, klass, "pixels", "[I");

    imageCnt = (*env)->GetArrayLength(env, in);
#ifdef REPEAT
  }
#endif
  stop_time("JNI Init");

  uint64_t img_init_total = 0;
  uint64_t img_conv_total = 0;

  for (int i = 0; i < imageCnt; i++) {
    jobject in_img = NULL;
    jobject out_img = NULL;
    jobject in_pix = NULL;
    jobject out_pix = NULL;
    int* in_pixels = NULL;
    int* out_pixels = NULL;
    int w=0, h=0;

    start_time();
#ifdef REPEAT
    for (int r=0;r<repeats;r++) {
#endif
      in_img = (*env)->GetObjectArrayElement(env, in, i);
      out_img = (*env)->GetObjectArrayElement(env, out, i);
      w = (*env)->GetIntField(env, in_img, wFID);
      h = (*env)->GetIntField(env, in_img, hFID);
      in_pix = (*env)->GetObjectField(env, in_img, pixelsFID);
      out_pix = (*env)->GetObjectField(env, out_img, pixelsFID);
      in_pixels = (*env)->GetPrimitiveArrayCritical(env, in_pix, NULL); //&isCopy);
      out_pixels = (*env)->GetPrimitiveArrayCritical(env, out_pix, NULL); //&isCopy);
#ifdef REPEAT
    }
#endif
    img_init_total += diff_time();

    start_time();
    convolute((unsigned int*)in_pixels, (unsigned int*)out_pixels, coeffs, scale, 5, w, h);
    img_conv_total += diff_time();

    start_time();
#ifdef REPEAT
    for (int r=0;r<repeats;r++) {
#endif
      (*env)->ReleasePrimitiveArrayCritical(env, in_pix, in_pixels, JNI_ABORT);
      (*env)->ReleasePrimitiveArrayCritical(env, out_pix, out_pixels, JNI_ABORT);
#ifdef REPEAT
    }
#endif
    img_init_total += diff_time();
  }
  printf("%llu, %llu, ", (long long unsigned int)img_init_total, (long long unsigned int)img_conv_total);
  fflush(stdout);
}

JNIEXPORT void JNICALL Java_org_tudelft_ewi_ce_fpgaserialize_fpgaserialize_00024_testKMeans(
    JNIEnv * env,
    jobject this,
    jobject in,
    jint dims,
    jint centers,
    jobject out)
{
  start_time();

  void * vecArrayOop = resolveJNIHandle(in);
  KMVector_Array vecArray = get_KMVector_Array(vecArrayOop);

  float** cVecs = (float**) malloc(vecArray.size * sizeof(float*));
  cVecs[0] = (float*)  malloc(vecArray.size * dims * sizeof(float));
  for (int i=1; i<vecArray.size; i++)
    cVecs[i] = cVecs[i-1] + dims;

  stop_time("Reckless");
  start_time();

  for (int i=0;i < vecArray.size; i++) {
    KMVector vec = get_KMVector(vecArray.values[i]);
    memcpy(cVecs[i], vec.values.values, vec.values.size * sizeof(float));
  }

  /*
  for (int i=0;i<vecArray.size; i++) {
    for (int j=0;j<dims;j++) {
      printf("%f, ", cVecs[i][j]);
    }
    printf("\n");
  }
   */

  stop_time("Reckless");
  start_time();

  int* membership = (int*) malloc(vecArray.size * sizeof(int));
  int iters;
  //seq_kmeans(cVecs, dims, vecArray.size, centers, 0.001f, membership, &iters);
  omp_kmeans(1, cVecs, dims, vecArray.size, centers, 0.001f, membership);
  //printf("Took %d iters\n",iters);
  stop_time("Reckless");

  fflush(stdout);
}


JNIEXPORT void JNICALL Java_org_tudelft_ewi_ce_fpgaserialize_fpgaserialize_00024_testKMeansJNI(
    JNIEnv * env,
    jobject this,
    jobject in,
    jint dims,
    jint centers,
    jobject out)
{

  start_time();

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

  stop_time("JNI");
  start_time();

  for (int i=0;i < objCnt; i++) {
    jobject obj = (*env)->GetObjectArrayElement(env, in, i);
    jarray vals = (*env)->GetObjectField(env, obj, valuesFID);
    float* coords = (*env)->GetPrimitiveArrayCritical(env, vals, NULL);
    assert(coords != NULL);
    memcpy(cVecs[i], coords, dims * sizeof(float));
    (*env)->ReleasePrimitiveArrayCritical(env, vals, coords, JNI_ABORT);
  }
  stop_time("JNI");
  start_time();

  int* membership = (int*) malloc(objCnt * sizeof(int));
  int iters;
  //seq_kmeans(cVecs, dims, objCnt, centers, 0.001f, membership, &iters);
  omp_kmeans(1, cVecs, dims, objCnt, centers, 0.001f, membership);
  //printf("Took %d iters\n",iters);
  stop_time("JNI");

  fflush(stdout);
}
