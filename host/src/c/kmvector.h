#ifndef SRC_KMVECTOR_H
#define SRC_KMVECTOR_H

typedef char byte;

typedef struct _float_Array float_Array;
typedef struct _KMVector KMVector;
typedef struct _KMVector_Array KMVector_Array;
inline float_Array get_float_Array(void* obj);
inline KMVector get_KMVector(void* obj);
inline KMVector_Array get_KMVector_Array(void* obj);
// STRUCTS: 

struct _float_Array {
  int                      _size;
  float*                   _values;
};

struct _KMVector {
  int                      size;
  float_Array              values;
};

struct _KMVector_Array {
  int                      _size;
  KMVector**               _values;
};

// INLINE FUNCTIONS: 

inline float_Array get_float_Array(void* obj) {
  float_Array ret;
  ret._size                = *(int*)((char*)obj + 16);
  ret._values              = (float*)((char*)obj + 24);
  return ret;
}

inline KMVector get_KMVector(void* obj) {
  KMVector ret;
  ret.size                 = *(int*)((char*)obj + 16);
  ret.values               = get_float_Array((void*)*(long*)((char*)obj + 24));
  return ret;
}

inline KMVector_Array get_KMVector_Array(void* obj) {
  KMVector_Array ret;
  ret._size                = *(int*)((char*)obj + 16);
  ret._values              = (KMVector**)((char*)obj + 24);
  return ret;
}


#endif //SRC_RECKLESS_H
