#include <jni.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <omp.h>

#include "fpgaserialize.h"
//#include "employees.h"
#include "pictures.h"

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



JNIEXPORT void JNICALL Java_org_tudelft_ewi_ce_fpgaserialize_fpgaserialize_00024_testPictures(JNIEnv * env, jobject this, jobject obj) {
  void* oop = resolveJNIHandle(obj);
  SimpleImage_Array* imgs = get_SimpleImage_Array(oop);
  printf("There are %d images\n", *imgs->size);
  for (int i = 0; i < *imgs->size; i++) {
    SimpleImage* img = get_SimpleImage(imgs->values[i]);
    for (int y = 0; y < *img->h; y++) {
      for (int x = 0; x < *img->w; x++) {
        img->pixels->values[y*(*img->w)+x] *= 2;
      }
    }
  }
}


JNIEXPORT void JNICALL Java_org_tudelft_ewi_ce_fpgaserialize_fpgaserialize_00024_testPicturesJNI(JNIEnv * env, jobject this, jobject obj) {
  jclass klass = (*env)->FindClass(env, "org/tudelft/ewi/ce/fpgaserialize/SimpleImage");

  jfieldID wFID = (*env)->GetFieldID(env, klass, "w", "I");
  jfieldID hFID = (*env)->GetFieldID(env, klass, "h", "I");
  jfieldID pixelsFID = (*env)->GetFieldID(env, klass, "pixels", "[I");

  jsize imageCnt = (*env)->GetArrayLength(env, obj);
  for (int i = 0; i < imageCnt; i++) {
    jobject img = (*env)->GetObjectArrayElement(env, obj, i);
    int w = (*env)->GetIntField(env, img, wFID);
    int h = (*env)->GetIntField(env, img, hFID);
    jobject pixelsArray = (*env)->GetObjectField(env, img, pixelsFID);
    jboolean isCopy;
    int* pixels = (*env)->GetIntArrayElements(env, pixelsArray, &isCopy);
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        pixels[y*w+x] *= 2;
      }
    }
    (*env)->ReleaseIntArrayElements(env, pixelsArray, pixels, 0);
  }
}

/*JNIEXPORT void JNICALL Java_org_tudelft_ewi_ce_fpgaserialize_fpgaserialize_00024_raiseSalary(JNIEnv * env, jobject this, jobject obj) {
  void* oop = resolveJNIHandle(obj);
  Employee_Array* emps = get_Employee_Array(oop);
  printf("Number of employees: %d\n", *emps->size);

  for (int i = 0; i < *emps->size; i++) {
    Employee* emp = get_Employee(emps->values[i]);
    *emp->salary += 1000;
    *emp->age = 50;
  }
}


JNIEXPORT void JNICALL Java_org_tudelft_ewi_ce_fpgaserialize_fpgaserialize_00024_test(JNIEnv * env, jobject this, jobject obj) {
  void* oop = resolveJNIHandle(obj);
  Employee* emp = get_Employee(oop);

  printf("Employee name: ");
  for (int i=0;i<*emp->name->value->size;i++)
    putchar(emp->name->value->values[i]);
  putchar('\n');

  printf("Employee age : %d\n", *emp->age);
}
*/

JNIEXPORT void JNICALL Java_org_tudelft_ewi_ce_fpgaserialize_SerializerSimulator_00024_printObjectMemory(JNIEnv * env, jobject this, jobject handle)
{
  //fprintf(stderr, "JNI Handle: %016lX - Data: %016lX\n", (long)handle, *(long*)handle);

  // JNI functions use JNI handles which are not OOPs. It must first be resolved.
  char* oop = resolveJNIHandle(handle);
  char* klass = getInstanceKlass(oop);
  int array = isArray(klass);
  int header_size = 16 + 4 * array;
  int size = getInstanceSizeOrElementSize(klass);
  //char * super = getSuperKlass(klass);
  int elementsize = size;
  int array_elements = 0;
  int header_gap = 0;
  if (array) {
    array_elements = getArrayElements(oop);
    header_gap = elementsize - (header_size % elementsize);
    header_size = header_size + header_gap;
    size = size * array_elements + header_size;
  }

  /*while ((super != NULL) && (array == 0)) {
    int super_size = getInstanceSizeOrElementSize(super);
    fprintf(stderr, "Super size: %d\n", super_size);
    array = isArray(super);
    super = getSuperKlass(super);
  }
  */

  fprintf(stdout, "OOP: %016lX\n", (long)oop);
  //fprintf(stderr, "Klass: %016lX\n", (long)klass);
  //fprintf(stderr, "Super: %016lX\n", (long)super);
  //fprintf(stderr, "Array: %d\n", array);
  //fprintf(stderr, "Header size: %d\n", header_size);
  //fprintf(stderr, "Size: %d\n", size);
  //if (array) {
//    fprintf(stderr, "Element size: %d\n", elementsize);
    //fprintf(stderr, "Array Elements: %d\n", array_elements);
    //fprintf(stderr, "Header alignment gap: %d\n", header_gap);
  //}

  printHexEditorView(stdout, oop, size);

  /*fprintf(stderr, "InstanceKlass pointer: %016lX\n", *(long*)&oop[8]);
  fprintf(stderr, "InstanceKlass contents:\n");
  printHexEditorView(stderr, klass, 128);
  fprintf(stderr, "Instance size: %08X\n", size);

  fprintf(stderr, "\n");*/

  fflush(stderr);

  return;
}
