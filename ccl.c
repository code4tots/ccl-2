/** gcc -Wall -Werror -Wpedantic -std=c89 ccl.c && ./a.out */
/** header */
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

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

typedef struct CCL_str CCL_str;
typedef struct CCL_list CCL_list;
typedef struct CCL_node CCL_node; /* node for dict */
typedef struct CCL_dict CCL_dict; /* implemented as a self balancing tree */
typedef struct CCL_Object CCL_Object;

struct CCL_str {
  size_t size, capacity;
  char *buffer;
};

struct CCL_list {
  size_t size, capacity;
  CCL_Object **buffer;
};

struct CCL_node {
  CCL_Object *key, *value;
  CCL_node *children[2];
  int color;
};

struct CCL_dict {
  size_t size;
  CCL_node *root;
};

struct CCL_Object {
  int type;
  union {
    int as_bool;
    double as_num;
    CCL_str as_str;
    CCL_list as_list;
    CCL_dict as_dict;
  } value;
};

int CCL_cmp(CCL_Object *left, CCL_Object *right);

/** static function headers */

static int dict_find(CCL_dict *dict, CCL_Object *key, CCL_node ***nodeptr, CCL_node **parent, int *childtype);
static int node_find(CCL_Object *key, CCL_node ***nodeptr, CCL_node **parent, int *childtype);

/** implementation */

int CCL_cmp(CCL_Object *left, CCL_Object *right) {
  /* TODO */
  if (left == right)
    return 0;

  if (left->type != right->type)
    return left->type - right->type;

  switch(left->type) {
  case CCL_NIL: return 0;
  case CCL_BOOL: return (left->value.as_bool && right->value.as_bool) || (!left->value.as_bool && !right->value.as_bool);
  case CCL_NUM: return left->value.as_num == right->value.as_num;
  case CCL_STR: return strcmp(left->value.as_str.buffer, right->value.as_str.buffer);
  case CCL_LIST: /* TODO */
  case CCL_DICT: /* TODO */
  default: return 0;
  }
}

static int dict_find(CCL_dict *dict, CCL_Object *key, CCL_node ***nodeptr, CCL_node **parent, int *childtype) {
  *nodeptr = &dict->root;
  return node_find(key, nodeptr, parent, childtype);
}

static int node_find(CCL_Object *key, CCL_node ***nodeptr, CCL_node **parent, int *childtype) {
  int cmp;

  while (**nodeptr != NULL) {
    cmp = CCL_cmp(key, (**nodeptr)->key);

    if (cmp == 0)
      return 1;

    *parent = **nodeptr;
    *nodeptr = &(*parent)->children[*childtype = cmp < 0 ? CCL_LEFT : CCL_RIGHT];
  }

  return 0;
}

/** tests */
#include <assert.h>

void CCL_test() {
  /* dict_find test */
  {
    CCL_dict d;
    CCL_node **n;
    d.root = NULL;
    assert(dict_find(&d, NULL, &n, NULL, NULL) == 0);
  }
  {
    CCL_dict d;
    CCL_node **result, a, b;

  }
}

/** main */

void CCL_test();

int main(int argc, char **argv) {
  CCL_test();
  return 0;
}
