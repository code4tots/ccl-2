#ifndef another_h
#define another_h

typedef struct CCL_Value CCL_Value;
typedef struct CCL_Int CCL_Int;
typedef struct CCL_Float CCL_Float;
typedef struct CCL_String CCL_String;
typedef struct CCL_List CCL_List;
typedef struct CCL_Map CCL_Map;

extern CCL_Value CCL_nil;
extern CCL_Value CCL_true;
extern CCL_Value CCL_false;
extern CCL_Map CCL_Meta_Nil;
extern CCL_Map CCL_Meta_Bool;
extern CCL_Map CCL_Meta_Int;
extern CCL_Map CCL_Meta_Float;
extern CCL_Map CCL_Meta_String;
extern CCL_Map CCL_Meta_List;
extern CCL_Map CCL_Meta_Map;

struct CCL_Value {
  CCL_Map *meta;
};

struct CCL_Int {
  CCL_Map *meta;
  int size, *buffer, sign;
};

struct CCL_Float {
  CCL_Map *meta;
  double value;
};

struct CCL_String {
  CCL_Map *meta;
  int hash, size;
  char *buffer;
};

struct CCL_List {
  CCL_Map *meta;
  int size, capacity;
  CCL_Value **buffer;
};

struct CCL_Map {
  CCL_Map *meta;
  int size, capacity;
  CCL_Value **buffer;
};

#endif/*another_h*/
