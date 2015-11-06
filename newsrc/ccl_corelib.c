#include "ccl.h"

#include <math.h>
#include <stdio.h>
#include <string.h>

#define MIN_LIST_CAPACITY 10

typedef struct CCL_Data_Str CCL_Data_Str;
typedef struct CCL_Data_List CCL_Data_List;

struct CCL_Data_Str {
  int size;
  char *buffer;
};

struct CCL_Data_List {
  int size, capacity;
  CCL_Object **buffer;
};

static CCL_Object *method_Object___str__(CCL_Object*, int, CCL_Object**);
static CCL_Object *method_Object___repr__(CCL_Object*, int, CCL_Object**);
static CCL_Object *method_Nil___repr__(CCL_Object*, int, CCL_Object**);
static CCL_Object *method_Bool___bool__(CCL_Object*, int, CCL_Object**);
static CCL_Object *method_Bool___repr__(CCL_Object*, int, CCL_Object**);
static CCL_Object *method_Num___cmp__(CCL_Object*, int, CCL_Object**);
static CCL_Object *method_Num___repr__(CCL_Object*, int, CCL_Object**);

static const CCL_Method methods_Object[] = {
  {"__str__", &method_Object___str__},
  {"__repr__", &method_Object___repr__}
};

static const CCL_Method methods_Nil[] = {
  {"__repr__", &method_Nil___repr__}
};

static const CCL_Method methods_Bool[] = {
  {"__bool__", &method_Bool___bool__},
  {"__repr__", &method_Bool___repr__}
};

static const CCL_Method methods_Num[] = {
  {"__cmp__", &method_Num___cmp__},
  {"__repr__", &method_Num___repr__}
};

static CCL_Class *bases_Nil[] = {
  CCL_Class_Object
};

CCL_Class CCL_s_Class_Object = {
  "Object",
  0, NULL, /* bases */
  0, NULL, /* direct attributes */
  sizeof(methods_Object)/sizeof(CCL_Method), methods_Object, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Class CCL_s_Class_Nil = {
  "Nil",
  sizeof(bases_Nil)/sizeof(CCL_Class*), bases_Nil, /* bases */
  0, NULL, /* direct attributes */
  sizeof(methods_Nil)/sizeof(CCL_Method), methods_Nil, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Class CCL_s_Class_Bool = {
  "Bool",
  sizeof(bases_Nil)/sizeof(CCL_Class*), bases_Nil, /* bases */
  0, NULL, /* direct attributes */
  sizeof(methods_Bool)/sizeof(CCL_Method), methods_Bool, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Class CCL_s_Class_Num = {
  "Num",
  sizeof(bases_Nil)/sizeof(CCL_Class*), bases_Nil, /* bases */
  0, NULL, /* direct attributes */
  sizeof(methods_Num)/sizeof(CCL_Method), methods_Num, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Class CCL_s_Class_Str = {
  "Str",
  sizeof(bases_Nil)/sizeof(CCL_Class*), bases_Nil,
  0, NULL, /* direct attributes */
  0, NULL, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Class CCL_s_Class_List = {
  "List",
  sizeof(bases_Nil)/sizeof(CCL_Class*), bases_Nil,
  0, NULL, /* direct attributes */
  0, NULL, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Object CCL_s_nil = {CCL_Class_Nil, {NULL}};
CCL_Object CCL_s_true = {CCL_Class_Bool, {NULL}};
CCL_Object CCL_s_false = {CCL_Class_Bool, {NULL}};

static CCL_Object *method_Object___str__(CCL_Object *me, int argc, CCL_Object **argv) {
  CCL_expect_argument_size(0, argc);
  return CCL_argv_invoke_method(me, "__repr__", argc, argv);
}

static CCL_Object *method_Object___repr__(CCL_Object *me, int argc, CCL_Object **argv) {
  char buffer[100]; /* Should fit any reasonble pointer representation.
                     * Although, technically speaking, '%p' output seems to be
                     * implementation defined, so who knows...
                     * TODO: figure out a safer alternative.
                     * Maybe by assigning all objects a UID. */

  CCL_expect_argument_size(0, argc);
  sprintf(buffer, "<%s instance at %p>", me->cls->name, me);

  return CCL_new_Str(buffer);
}

static CCL_Object *method_Nil___repr__(CCL_Object *me, int argc, CCL_Object **argv) {
  CCL_expect_argument_size(0, argc);
  return CCL_new_Str("nil");
}

static CCL_Object *method_Bool___bool__(CCL_Object *me, int argc, CCL_Object **argv) {
  CCL_expect_argument_size(0, argc);
  return me;
}

static CCL_Object *method_Bool___repr__(CCL_Object *me, int argc, CCL_Object **argv) {
  CCL_expect_argument_size(0, argc);
  return CCL_new_Str(me == CCL_true ? "true" : "false");
}

static CCL_Object *method_Num___cmp__(CCL_Object *me, int argc, CCL_Object **argv) {
  CCL_expect_argument_size(1, argc);

  if (argv[0]->cls != CCL_Class_Num)
    return CCL_new_Num(strcmp(CCL_Class_Num->name, argv[0]->cls->name));

  return CCL_new_Num(CCL_Num_value(me) - CCL_Num_value(argv[0]));
}

static CCL_Object *method_Num___repr__(CCL_Object *me, int argc, CCL_Object **argv) {
  double value = CCL_Num_value(me);
  char buffer[30]; /* should fit both 64-bit integers, and %.20G specified below */

  CCL_expect_argument_size(0, argc);

  if (floor(value) == value)
    sprintf(buffer, "%ld", (long) value);

  else
    sprintf(buffer, "%.20G", value);


  return CCL_new_Str(buffer);
}

CCL_Object *CCL_new_Num(double value) {
  CCL_Object *me = CCL_alloc(CCL_Class_Num);
  double *data = CCL_malloc(sizeof(double));
  *data = value;
  me->pointer_to.raw_data = data;
  return me;
}

CCL_Object *CCL_new_Str(const char *value) {
  CCL_Object *me;
  int size = strlen(value);
  char *buffer = CCL_malloc(sizeof(char) * (size+1));

  strcpy(buffer, value);

  me = CCL_alloc(CCL_Class_Str);
  me->pointer_to.raw_data = CCL_malloc(sizeof(CCL_Data_Str));

  ((CCL_Data_Str*) me->pointer_to.raw_data)->size = size;
  ((CCL_Data_Str*) me->pointer_to.raw_data)->buffer = buffer;

  return me;
}

CCL_Object *CCL_new_List(int argc, ...) {
  int i, capacity = argc < MIN_LIST_CAPACITY ? MIN_LIST_CAPACITY : argc;
  CCL_Object *me = CCL_alloc(CCL_Class_List);
  CCL_Data_List *data = CCL_malloc(sizeof(CCL_Data_List));
  va_list ap;

  data->size = argc;
  data->capacity = capacity;
  data->buffer = CCL_malloc(sizeof(CCL_Object*) * capacity);

  me->pointer_to.raw_data = data;

  va_start(ap, argc);
  for (i = 0; i < argc; i++)
    data->buffer[i] = va_arg(ap, CCL_Object*);
  va_end(ap);

  return me;
}

int CCL_truthy(CCL_Object *me) {
  CCL_Object *truthy = CCL_invoke_method(me, "__bool__", 0);

  CCL_assert(
      truthy->cls == CCL_Class_Bool,
      "__bool__ must return a Bool result but returned object of type '%s'",
      truthy->cls->name);

  return truthy == CCL_true;
}

const char *CCL_repr(CCL_Object *me) {
  CCL_Object *repr = CCL_invoke_method(me, "__repr__", 0);

  CCL_assert(
      repr->cls == CCL_Class_Str,
      "__repr__ must return a Str result but returned object of type '%s'",
      repr->cls->name);

  return CCL_Str_buffer(repr);
}

double CCL_Num_value(CCL_Object *me) {
  CCL_assert(
      me->cls == CCL_Class_Num,
      "CCL_Num_value requires a Num argument but found '%s'",
      me->cls->name);

  return *(double*) me->pointer_to.raw_data;
}

const char *CCL_Str_buffer(CCL_Object *me) {
  CCL_assert(
      me->cls == CCL_Class_Str,
      "CCL_Str_buffer requires a Str argument but found '%s'",
      me->cls->name);

  return ((CCL_Data_Str*) me->pointer_to.raw_data)->buffer;
}

int CCL_Str_size(CCL_Object *me) {
  CCL_assert(
      me->cls == CCL_Class_Str,
      "CCL_Str_size requires a Str argument but found '%s'",
      me->cls->name);

  return ((CCL_Data_Str*) me->pointer_to.raw_data)->size;
}

CCL_Object *const *CCL_List_buffer(CCL_Object *me) {
  CCL_assert(
      me->cls == CCL_Class_List,
      "CCL_List_buffer requires a List argument but found '%s'",
      me->cls->name);

  return ((CCL_Data_List*) me->pointer_to.raw_data)->buffer;
}

int CCL_List_size(CCL_Object *me) {
  CCL_assert(
      me->cls == CCL_Class_List,
      "CCL_List_size requires a List argument but found '%s'",
      me->cls->name);

  return ((CCL_Data_List*) me->pointer_to.raw_data)->size;
}
