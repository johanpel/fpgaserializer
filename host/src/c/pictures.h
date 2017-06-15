#ifndef SRC_PICTURES_H
#define SRC_PICTURES_H

typedef char byte;

typedef struct _int_Array int_Array;
typedef struct _SimpleImage SimpleImage;
typedef struct _SimpleImage_Array SimpleImage_Array;
inline int_Array* get_int_Array(void* obj);
inline SimpleImage* get_SimpleImage(void* obj);
inline SimpleImage_Array* get_SimpleImage_Array(void* obj);
// TYPE DEFINITIONS: 

struct _int_Array {
  int*                     size;
  int*                     values;
};

struct _SimpleImage {
  int*                     w;
  int*                     h;
  int_Array*               pixels;
};

struct _SimpleImage_Array {
  int*                     size;
  SimpleImage**            values;
};

// STRUCTURE FILLING FUNCTIONS: 

inline int_Array* get_int_Array(void* obj) {
  int_Array* ret           = (int_Array*)malloc(sizeof(int_Array));
  ret->size                = (int*)((char*)obj + 16);
  ret->values              = (int*)((char*)obj + 24);
  return ret;
}

inline SimpleImage* get_SimpleImage(void* obj) {
  SimpleImage* ret         = (SimpleImage*)malloc(sizeof(SimpleImage));
  ret->w                   = (int*)((char*)obj + 16);
  ret->h                   = (int*)((char*)obj + 20);
  ret->pixels              = get_int_Array((void*)*(long*)((char*)obj + 24));
  return ret;
}

inline SimpleImage_Array* get_SimpleImage_Array(void* obj) {
  SimpleImage_Array* ret   = (SimpleImage_Array*)malloc(sizeof(SimpleImage_Array));
  ret->size                = (int*)((char*)obj + 16);
  ret->values              = (SimpleImage**)((char*)obj + 24);
  return ret;
}


#endif //SRC_RECKLESS_H
