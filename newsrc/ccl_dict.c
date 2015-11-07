#include "ccl.h"

#define LEFT 0
#define RIGHT 1

typedef struct DictPointer DictPointer;
typedef struct CCL_Data_Dict CCL_Data_Dict;

struct DictPointer {
  CCL_Data_Dict *parent, **pointer_to_node;
  int direction;
};

struct CCL_Data_Dict {
  int size;
  CCL_Data_Dict *parent, *children[2];
  CCL_Object *key, *value;
};

static CCL_Object *method_Dict___size__(CCL_Object*, int, CCL_Object**);
static CCL_Object *method_Dict___contains__(CCL_Object*, int, CCL_Object**);
static CCL_Object *method_Dict___setitem__(CCL_Object*, int, CCL_Object**);
static CCL_Object *method_Dict___getitem__(CCL_Object*, int, CCL_Object**);

static const CCL_Method methods_Dict[] = {
  {"__size__", &method_Dict___size__},
  {"__contains__", &method_Dict___contains__},
  {"__setitem__", &method_Dict___setitem__},
  {"__getitem__", &method_Dict___getitem__}
};

static CCL_Class *bases_Dict[] = {
  CCL_Class_Object
};

CCL_Class CCL_s_Class_Dict = {
  "Dict",
  sizeof(bases_Dict)/sizeof(CCL_Class*), bases_Dict,
  0, NULL, /* direct attributes */
  sizeof(methods_Dict)/sizeof(CCL_Method), methods_Dict, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

static DictPointer makeRootDictPointer(CCL_Object *me) {
  DictPointer dp;

  dp.parent = NULL;
  dp.pointer_to_node = (CCL_Data_Dict**) &me->pointer_to.raw_data;
  dp.direction = 0;

  return dp;
}

static DictPointer makeChildDictPointer(DictPointer dp, int direction) {
  DictPointer cdp;

  cdp.parent = *dp.pointer_to_node;
  cdp.pointer_to_node = &cdp.parent->children[direction];
  dp.direction = direction;

  return cdp;
}

static DictPointer Dict_find(CCL_Object *me, CCL_Object *key) {
  DictPointer dp = makeRootDictPointer(me);

  while (*dp.pointer_to_node) {
    CCL_Data_Dict *d = *dp.pointer_to_node;
    int cmp = CCL_Num_value(CCL_invoke_method(key, "__cmp__", 1, d->key));

    if (cmp == 0)
      break;

    dp = makeChildDictPointer(dp, cmp < 0 ? LEFT : RIGHT);
  }

  return dp;
}

static void Dict_setitem(CCL_Object *me, CCL_Object *key, CCL_Object *value) {
  DictPointer dp = Dict_find(me, key);
  CCL_Data_Dict *d, *p;

  if (*dp.pointer_to_node == NULL) {
    d = CCL_malloc(sizeof(CCL_Data_Dict));
    d->size = 1;
    d->parent = dp.parent;
    d->children[LEFT] = d->children[RIGHT] = NULL;
    d->key = key;

    *dp.pointer_to_node = d;

    for (p = d->parent; p != NULL; p = p->parent)
      p->size++;

    /* TODO: balance the tree here */
  }
  else
    d = *dp.pointer_to_node;

  d->value = value;
}

static CCL_Object *Dict_getitem(CCL_Object *me, CCL_Object *key) {
  DictPointer dp = Dict_find(me, key);

  return *dp.pointer_to_node == NULL ? NULL : (*dp.pointer_to_node)->value;
}

static CCL_Object *method_Dict___size__(CCL_Object *me, int argc, CCL_Object **argv) {
  CCL_expect_argument_size(0, argc);
  return CCL_new_Num(CCL_Dict_size(me));
}

static CCL_Object *method_Dict___contains__(CCL_Object *me, int argc, CCL_Object **argv) {
  CCL_expect_argument_size(1, argc);

  return Dict_getitem(me, argv[0]) != NULL ? CCL_true : CCL_false;
}

static CCL_Object *method_Dict___setitem__(CCL_Object *me, int argc, CCL_Object **argv) {
  CCL_expect_argument_size(2, argc);
  Dict_setitem(me, argv[0], argv[1]);
  return argv[1];
}

static CCL_Object *method_Dict___getitem__(CCL_Object *me, int argc, CCL_Object **argv) {
  CCL_Object *value;

  CCL_expect_argument_size(1, argc);
  value = Dict_getitem(me, argv[0]);

  if (value == NULL)
    CCL_err("Key %s not found", CCL_repr(argv[0]));

  return value;
}

CCL_Object *CCL_new_Dict(int argc, ...) {
  CCL_Object *me;
  va_list ap;
  int i;

  CCL_assert(argc % 2 == 0, "CCL_new_Dict requires an even number of arguments");

  me = CCL_alloc(CCL_Class_Dict);
  me->pointer_to.raw_data = NULL;

  va_start(ap, argc);
  for (i = 0; i < argc; i += 2) {
    CCL_Object *key = va_arg(ap, CCL_Object*);
    CCL_Object *value = va_arg(ap, CCL_Object*);
    Dict_setitem(me, key, value);
  }
  va_end(ap);

  return me;
}

int CCL_Dict_size(CCL_Object *me) {
  CCL_assert(
      me->cls == CCL_Class_Dict,
      "CCL_Dict_size requires a Dict argument but found '%s'",
      me->cls->name);

  if (me->pointer_to.raw_data == NULL)
    return 0;

  return ((CCL_Data_Dict*) me->pointer_to.raw_data)->size;
}
