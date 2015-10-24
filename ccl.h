/** header */
#include <assert.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define CCL_RED 0
#define CCL_BLACK 1

#define CCL_LEFT 0
#define CCL_RIGHT 1

#define CCL_NIL 0
#define CCL_BOOL 1
#define CCL_NUM 2
#define CCL_STR 3
#define CCL_LIST 4
#define CCL_DICT 5
#define CCL_FUNC 6
#define CCL_LAMBDA 7

typedef struct CCL_str CCL_str;
typedef struct CCL_list CCL_list;
typedef struct CCL_dict CCL_dict;
typedef struct CCL_lambda CCL_lambda;
typedef struct CCL_Object CCL_Object;

struct CCL_str {
  size_t size;
  const char *buffer;
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

struct CCL_lambda {
  CCL_Object *context, *node;
};

struct CCL_Object {
  int type;
  union {
    int as_bool;
    double as_num;
    CCL_str as_str;
    CCL_list as_list;
    CCL_dict *as_dict;
    CCL_Object *(*as_func)(int, ...);
    CCL_lambda as_lambda;
  } value;
};

extern CCL_Object *CCL_nil;
extern CCL_Object *CCL_true;
extern CCL_Object *CCL_false;

void CCL_init(); /* must be called before any other things */
int CCL_cmp(CCL_Object *left, CCL_Object *right);
CCL_Object *CCL_num_new(double value);
CCL_Object *CCL_str_new(const char *value);
CCL_Object *CCL_list_new(int argc, ...);
void CCL_list_add(CCL_Object *list, CCL_Object *item);
CCL_Object *CCL_list_pop(CCL_Object *list);
CCL_Object *CCL_dict_new(int argc, ...);
size_t CCL_dict_size(CCL_Object *dict);
void CCL_dict_setitem(CCL_Object *dict, CCL_Object *key, CCL_Object *value);
void CCL_dict_delitem(CCL_Object *dict, CCL_Object *key);
CCL_Object *CCL_dict_getitem(CCL_Object *dict, CCL_Object *key);

CCL_Object *CCL_strcat(CCL_Object *list_of_str);
