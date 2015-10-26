/* ccl.h */
#include <assert.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

enum CCL_Type {
  CCL_NIL,
  CCL_BOOL,
  CCL_NUM,
  CCL_STR,
  CCL_LIST,
  CCL_DICT,
  CCL_FUNC,
};

enum CCL_Bool {
  CCL_FALSE,
  CCL_TRUE
};

typedef enum CCL_Type CCL_Type;
typedef enum CCL_Bool CCL_Bool;
typedef struct CCL_str CCL_str;
typedef struct CCL_list CCL_list;
typedef struct CCL_dict CCL_dict;
typedef struct CCL_func CCL_func;
typedef struct CCL_Object CCL_Object;

struct CCL_str {
  size_t size;
  char *buffer;
};

struct CCL_list {
  size_t size, capacity;
  CCL_Object **buffer;
};

struct CCL_dict {
  CCL_Object *key, *value;
  CCL_dict *children[2];
  size_t size;
};

struct CCL_func {
  CCL_Object *context, *argument_names, *body;
};

struct CCL_Object {
  CCL_Type type;
  union {
    CCL_Bool as_bool;
    double as_num;
    CCL_str as_str;
    CCL_list as_list;
    CCL_dict *as_dict;
    CCL_func as_func;
  } value;
};
