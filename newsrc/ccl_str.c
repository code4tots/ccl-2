#include "ccl.h"

typedef struct CCL_Data_Str CCL_Data_Str;

struct CCL_Data_Str {
  int size;
  char *buffer;
};

static CCL_Class *bases_Str[] = {
  CCL_Class_Object
};

CCL_Class CCL_s_Class_Str = {
  "Str",
  sizeof(bases_Str)/sizeof(CCL_Class*), bases_Str,
  0, NULL, /* direct attributes */
  0, NULL, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

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
