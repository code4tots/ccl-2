/** gcc -Wall -Werror -Wpedantic -std=c89 ccl.c && ./a.out */
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
void CCL_dict_setitem(CCL_Object *dict, CCL_Object *key, CCL_Object *value);
void CCL_dict_delitem(CCL_Object *dict, CCL_Object *key);
CCL_Object *CCL_dict_getitem(CCL_Object *dict, CCL_Object *key);

CCL_Object *CCL_strcat(CCL_Object *list_of_str);

/** static function headers */

/** implementation */

#define CCL_MAX_INTERNED_NUM 200
#define CCL_MIN_LIST_SIZE 10

static int CCL_initialized = 0;

static CCL_Object CCL_nil_object = { CCL_NIL };
CCL_Object *CCL_nil = &CCL_nil_object;
static CCL_Object CCL_true_object = {CCL_BOOL, {1}};
CCL_Object *CCL_true = &CCL_true_object;
static CCL_Object CCL_false_object = {CCL_BOOL, {0}};
CCL_Object *CCL_false = &CCL_false_object;

static CCL_Object nonnegative_nums[CCL_MAX_INTERNED_NUM];
static CCL_Object negative_nums[CCL_MAX_INTERNED_NUM];

void CCL_init() {
  int i;

  if (CCL_initialized)
    return;

  CCL_initialized = 1;

  for (i = 0; i < CCL_MAX_INTERNED_NUM; i++) {
    nonnegative_nums[i].type = CCL_NUM;
    nonnegative_nums[i].value.as_num = i;
    negative_nums[i].type = CCL_NUM;
    negative_nums[i].value.as_num = -i;
  }
}

int CCL_cmp(CCL_Object *left, CCL_Object *right) {
  if (left == right)
    return 0;

  if (left->type != right->type)
    return left->type - right->type;

  switch(left->type) {
  case CCL_NIL: return 0;
  case CCL_BOOL: return left->value.as_bool - right->value.as_bool;
  case CCL_NUM: return left->value.as_num - right->value.as_num;
  case CCL_STR: return strcmp(left->value.as_str.buffer, right->value.as_str.buffer);
  case CCL_LIST: {
    int i, cmp, size = left->value.as_list.size < right->value.as_list.size ? left->value.as_list.size : right->value.as_list.size;
    for (i = 0; i < size; i++) {
      cmp = CCL_cmp(left->value.as_list.buffer[i], right->value.as_list.buffer[i]);
      if (cmp != 0)
        return cmp;
    }
    return left->value.as_list.size - right->value.as_list.size;
  }
  case CCL_DICT: /* TODO */
  case CCL_FUNC:
  case CCL_LAMBDA: return (char*) left - (char*) right;
  default:
    fprintf(stderr, "Invalid type: %d", left->type);
    exit(1);
  }
}

CCL_Object *CCL_num_new(double value) {
  CCL_Object *num;

  if (value == (int) value && value < CCL_MAX_INTERNED_NUM && -CCL_MAX_INTERNED_NUM < value)
    return value < 0 ? &negative_nums[-(int) value] : &nonnegative_nums[(int) value];

  num = malloc(sizeof(CCL_Object));
  num->type = CCL_NUM;
  num->value.as_num = value;
  return num;
}

CCL_Object *CCL_str_new(const char *value) {
  CCL_Object *str = malloc(sizeof(CCL_Object));
  size_t size = strlen(value);
  char *buffer = malloc(sizeof(char) * (size+1));

  strcpy(buffer, value);

  str->type = CCL_STR;
  str->value.as_str.size = size;
  str->value.as_str.buffer = buffer;

  return str;
}

CCL_Object *CCL_list_new(int argc, ...) {
  CCL_Object *list = malloc(sizeof(CCL_Object));
  int i;
  va_list ap;
  va_start(ap, argc);

  list->type = CCL_LIST;
  list->value.as_list.size = argc;
  list->value.as_list.capacity = argc < CCL_MIN_LIST_SIZE ? CCL_MIN_LIST_SIZE : argc;
  list->value.as_list.buffer = malloc(sizeof(CCL_Object*) * list->value.as_list.capacity);

  for (i = 0; i < argc; i++)
    list->value.as_list.buffer[i] = va_arg(ap ,CCL_Object*);

  va_end(ap);
  return list;
}

void CCL_list_add(CCL_Object *list, CCL_Object *item) {
  if (list->value.as_list.size == list->value.as_list.capacity) {
    list->value.as_list.capacity *= 2;
    list->value.as_list.buffer = realloc(list->value.as_list.buffer, sizeof(CCL_Object*) * list->value.as_list.capacity);
  }

  list->value.as_list.buffer[list->value.as_list.size++] = item;
}

CCL_Object *CCL_list_pop(CCL_Object *list) {
  return list->value.as_list.buffer[--list->value.as_list.size];
}

CCL_Object *CCL_dict_new(int argc, ...) {
  CCL_Object *dict = malloc(sizeof(CCL_Object));
  va_list ap;
  va_start(ap, argc);

  dict->type = CCL_DICT;
  dict->value.as_dict = NULL;

  assert(argc % 2 == 0);

  for (; argc; argc -= 2) {
    CCL_Object *key = va_arg(ap, CCL_Object*);
    CCL_Object *value = va_arg(ap, CCL_Object*);
    CCL_dict_setitem(dict, key, value);
  }

  va_end(ap);

  return dict;
}

size_t CCL_dict_size(CCL_Object *dict) {
  return dict->value.as_dict == NULL ? 0 : dict->value.as_dict->size;
}

void CCL_dict_setitem(CCL_Object *dict, CCL_Object *key, CCL_Object *value) {
  /* TODO: Weight balance the tree on insertion */
  int cmp, new_key = CCL_dict_getitem(dict, key) == NULL;
  CCL_dict **nodeptr = &dict->value.as_dict;

  while (*nodeptr != NULL && (cmp = CCL_cmp(key, (*nodeptr)->key)) != 0) {
    if (new_key)
      (*nodeptr)->size++;

    nodeptr = &(*nodeptr)->children[cmp < 0 ? CCL_LEFT : CCL_RIGHT];
  }

  if (*nodeptr == NULL) {
    (*nodeptr) = malloc(sizeof(CCL_dict));
    (*nodeptr)->size = 1;
    (*nodeptr)->children[0] =
    (*nodeptr)->children[1] = NULL;
    (*nodeptr)->key = key;
  }

  (*nodeptr)->value = value;
}

void CCL_dict_delitem(CCL_Object *dict, CCL_Object *key) {
  assert(0); /* TODO */
}

CCL_Object *CCL_dict_getitem(CCL_Object *dict, CCL_Object *key) {
  CCL_dict *node = dict->value.as_dict;

  assert(dict->type == CCL_DICT);

  while (node != NULL) {
    int cmp = CCL_cmp(key, node->key);

    if (cmp == 0)
      return node->value;

    node = node->children[cmp < 0 ? CCL_LEFT : CCL_RIGHT];
  }

  return NULL;
}

CCL_Object *CCL_strcat(CCL_Object *list_of_str) {
  size_t i, total_size = 0;
  char *buffer, *end;
  CCL_Object *ret;

  for (i = 0; i < list_of_str->value.as_list.size; i++) {
    CCL_Object *str = list_of_str->value.as_list.buffer[i];
    assert(str->type == CCL_STR);
    total_size += str->value.as_str.size;
  }

  buffer = end = malloc(sizeof(char) * (total_size+1));
  *end = '\0';

  for (i = 0; i < list_of_str->value.as_list.size; i++) {
    CCL_Object *str = list_of_str->value.as_list.buffer[i];
    strcpy(end, str->value.as_str.buffer);
    end += str->value.as_str.size;
  }

  ret = CCL_str_new(buffer);

  free(buffer);

  return ret;
}

/** tests */

void CCL_test() {
  /* num tests */
  {
    assert(CCL_num_new(0) == CCL_num_new(0));
    assert(CCL_num_new(0)->value.as_num == 0);
    assert(CCL_num_new(1)->value.as_num == 1);
  }
  /* str tests */
  {
    assert(CCL_str_new("abcdef")->value.as_str.size == 6);
    assert(CCL_strcat(CCL_list_new(3, CCL_str_new("abc"), CCL_str_new("def"), CCL_str_new("123")))->value.as_str.size == 9);
    assert(CCL_cmp(CCL_strcat(CCL_list_new(3, CCL_str_new("abc"), CCL_str_new("def"), CCL_str_new("123"))),
                   CCL_str_new("abcdef123")) == 0);
  }
  /* list tests */
  {
    CCL_Object *list1 = CCL_list_new(2, CCL_num_new(555), CCL_nil),
               *list2 = CCL_list_new(2, CCL_num_new(555), CCL_nil),
               *list3 = CCL_list_new(2, CCL_num_new(556), CCL_nil);
    assert(list1->value.as_list.size == 2);
    assert(CCL_cmp(list1, list2) == 0);
    assert(CCL_cmp(list1, list3) < 0);

    CCL_list_add(list1, CCL_true);
    assert(list1->value.as_list.size == 3);
    assert(list1->value.as_list.buffer[2] == CCL_true);
    assert(CCL_cmp(list1->value.as_list.buffer[2], CCL_true) == 0);
    assert(CCL_cmp(list1, list2) > 0);

    assert(CCL_list_pop(list1) == CCL_true);
    assert(CCL_cmp(list1, list2) == 0);
  }
  /* dict tests */
  {
    CCL_Object *dict = CCL_dict_new(0), *one = CCL_num_new(1), *two = CCL_num_new(2);

    assert(CCL_dict_size(dict) == 0);
    assert(CCL_dict_getitem(dict, CCL_nil) == NULL);

    CCL_dict_setitem(dict, CCL_nil, CCL_nil);
    assert(CCL_dict_size(dict) == 1);
    assert(CCL_dict_getitem(dict, CCL_nil) == CCL_nil);

    CCL_dict_setitem(dict, one, two);
    assert(CCL_dict_size(dict) == 2);
    assert(CCL_dict_getitem(dict, CCL_nil) == CCL_nil);
    assert(CCL_dict_getitem(dict, one) == two);

    CCL_dict_setitem(dict, CCL_nil, one);
    assert(CCL_dict_size(dict) == 2);
    assert(CCL_dict_getitem(dict, CCL_nil) == one);
    assert(CCL_dict_getitem(dict, one) == two);

    CCL_dict_setitem(dict, two, CCL_nil);
    assert(CCL_dict_size(dict) == 3);
    assert(CCL_dict_getitem(dict, CCL_nil) == one);
    assert(CCL_dict_getitem(dict, one) == two);
    assert(CCL_dict_getitem(dict, two) == CCL_nil);
    assert(CCL_dict_getitem(dict, CCL_true) == NULL);
  }
}

/** main */

void CCL_test();

int main(int argc, char **argv) {
  CCL_init();
  CCL_test();
  return 0;
}
