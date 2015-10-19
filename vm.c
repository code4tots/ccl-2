/* gcc vm.c -std=c89 -Wall -Werror -Wpedantic */
#include <stdarg.h>
#include <stdlib.h>
#include <string.h>

enum Type {
  TYPE_NIL,
  TYPE_BOOL,
  TYPE_NUM,
  TYPE_STR,
  TYPE_LIST,
  TYPE_DICT,
  TYPE_MACRO,
  TYPE_FUNC
};

typedef enum Type Type;
typedef struct Object Object;
typedef struct Pair Pair;

struct Pair {
  Object *key, *val;
};

struct Object {
  Type type;
  union {
    int as_bool;
    double as_num;
    struct {
      int size;
      char *buf;
    } as_str;
    struct {
      int size, cap;
      Object *buf;
    } as_list;
    struct {
      int size;
      Pair *pairs;
    } as_dict; /* TODO: implement more efficient dict */
    Object *(*as_macro)(Object *ctx, Object *args);
    Object *(*as_func)(Object *args);
  } val;
};

Object *allocate_Object(Type type) {
  Object *obj = malloc(sizeof(Object));
  obj->type = type;
  return obj;
}

Object *make_nil() {
  return allocate_Object(TYPE_NIL);
}

Object *make_bool(int val) {
  Object *obj = allocate_Object(TYPE_BOOL);
  obj->val.as_bool = val;
  return obj;
}

Object *make_str(const char *val) {
  Object *obj = allocate_Object(TYPE_STR);
  int size = strlen(val);
  obj->val.as_str.size = size;
  obj->val.as_str.buf = malloc(sizeof(char) * (size+1));
  strcpy(obj->val.as_str.buf, val);
  return obj;
}

Object *make_list(int size, ...) {
  Object *obj = allocate_Object(TYPE_LIST);
  obj->val.as_list.size = size;
  obj->val.as_list.cap = size;
  obj->val.as_list.buf = malloc(sizeof(Object*) * size);
  return obj;
}

int main(int argc, char **argv) {
  return 0;
}
