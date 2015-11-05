#include "ccl.h"

#include <string.h>

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

static CCL_Class *bases_Nil[] = {
  CCL_Class_Object
};

CCL_Class CCL_s_Class_Object = {
  "Object",
  0, NULL, /* bases */
  0, NULL, /* direct attributes */
  0, NULL, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Class CCL_s_Class_Nil = {
  "Nil",
  sizeof(bases_Nil)/sizeof(CCL_Class), bases_Nil,
  0, NULL, /* direct attributes */
  0, NULL, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Class CCL_s_Class_Bool = {
  "Bool",
  sizeof(bases_Nil)/sizeof(CCL_Class), bases_Nil,
  0, NULL, /* direct attributes */
  0, NULL, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Class CCL_s_Class_Num = {
  "Num",
  sizeof(bases_Nil)/sizeof(CCL_Class), bases_Nil,
  0, NULL, /* direct attributes */
  0, NULL, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Class CCL_s_Class_Str = {
  "Str",
  sizeof(bases_Nil)/sizeof(CCL_Class), bases_Nil,
  0, NULL, /* direct attributes */
  0, NULL, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Class CCL_s_Class_List = {
  "List",
  sizeof(bases_Nil)/sizeof(CCL_Class), bases_Nil,
  0, NULL, /* direct attributes */
  0, NULL, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Class CCL_s_Class_Dict = {
  "Dict",
  sizeof(bases_Nil)/sizeof(CCL_Class), bases_Nil,
  0, NULL, /* direct attributes */
  0, NULL, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Object CCL_s_nil = {CCL_Class_Nil, {NULL}};
CCL_Object CCL_s_true = {CCL_Class_Bool, {NULL}};
CCL_Object CCL_s_false = {CCL_Class_Bool, {NULL}};;

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

CCL_Object *CCL_new_List(int, ...);
CCL_Object *CCL_new_Dict(int, ...);

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

CCL_Object *const *CCL_List_buffer(CCL_Object*);
int CCL_List_size(CCL_Object*);
int CCL_Dict_size(CCL_Object*);
