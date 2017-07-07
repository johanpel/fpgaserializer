#include <stdint.h>
#include <jni.h>

#ifndef SRC_FPGASERIALIZE_H_
#define SRC_FPGASERIALIZE_H_

void printBytes(FILE * f, void * data, int bytes);
void printHexEditorView(FILE * f, void * data, int bytes);

static inline char * getSuperKlass(char * klass) {
  return (char*)*(long*)&klass[56];
}

static inline void* resolveJNIHandle(jobject handle) {
  long* result = NULL;
  if (handle != NULL)
    result = (long*)*(long*)handle;

  return (void*)result;
}

static inline char * getInstanceKlass(char * oop) {
  return (char*)*(long*)&oop[8];
}

static inline int getInstanceSizeOrElementSize(char * klass) {
  // Layout helper value
  uint32_t lh = *(int*)&klass[8];
  if ((lh & 0x80000000) == 0x80000000) {
    // it's an array, two to the power the last byte is the element size
    return 1 << (lh & 0x000000FF);
  } else {
    // it's a normal object, lh is the size in bytes
    return lh;
  }
}

static inline int isArray(char * klass) {
  uint32_t lh = *(int*)&klass[8];
  if ((lh & 0x80000000) == 0x80000000) return 1;
  else return 0;
}

static inline int getArrayElements(char * oop) {
  return *(int*)&oop[16];
}

JNIEXPORT void JNICALL Java_org_tudelft_ewi_ce_fpgaserialize_fpgaserialize_00024_testKMeans(
    JNIEnv * env,
    jobject me,
    jobjectArray in,
    jint dims,
    jint centers,
    jint mode,
    jarray out);

JNIEXPORT void JNICALL Java_org_tudelft_ewi_ce_fpgaserialize_fpgaserialize_00024_testKMeansJNI(
    JNIEnv * env,
    jobject me,
    jobjectArray in,
    jint dims,
    jint centers,
    jint mode,
    jarray out);

#endif /* SRC_FPGASERIALIZE_H_ */
