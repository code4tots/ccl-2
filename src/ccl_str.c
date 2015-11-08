#include "ccl.h"

#include <string.h>

typedef struct CCL_Data_Str CCL_Data_Str;

struct CCL_Data_Str {
  int size;
  char *buffer;
};

static CCL_Object *method_Str___cmp__(CCL_Object*, int, CCL_Object**);
static CCL_Object *method_Str___repr__(CCL_Object*, int, CCL_Object**);
static CCL_Object *method_Str___str__(CCL_Object*, int, CCL_Object**);

static const CCL_Method methods_Str[] = {
  {"__cmp__", &method_Str___cmp__},
  {"__repr__", &method_Str___repr__},
  {"__str__", &method_Str___str__},
};

static CCL_Class *bases_Str[] = {
  CCL_Class_Object
};

CCL_Class CCL_s_Class_Str = {
  "Str",
  sizeof(bases_Str)/sizeof(CCL_Class*), bases_Str,
  0, NULL, /* direct attributes */
  sizeof(methods_Str)/sizeof(CCL_Method), methods_Str, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

static char escchar(char c) {
  switch(c) {
  case '\n': return 'n';
  case '\t': return 't';
  case '\\': return '\\';
  case '\"': return '\"';
  default: return 0;
  }
}

static CCL_Object *method_Str___cmp__(CCL_Object *me, int argc, CCL_Object **argv) {
  CCL_expect_argument_size(1, argc);
  if (argv[0]->cls != CCL_Class_Str)
    return CCL_typecmp(me, argv[0]);
  return me;
}

static CCL_Object *method_Str___repr__(CCL_Object *me, int argc, CCL_Object **argv) {
  const char *str;
  char *buf;
  int i, j, len, rlen;
  CCL_Object *repr;

  CCL_expect_argument_size(0, argc);

  str = CCL_Str_buffer(me);
  len = strlen(str);

  for (i = rlen = 0; i < len; i++)
    rlen += escchar(str[i]) ? 2 : 1;

  buf = CCL_malloc(sizeof(char) * (rlen+3));

  buf[0] = '"';
  for (i = 0, j = 1; i < len; i++) {
    if (escchar(str[i])) {
      buf[j++] = '\\';
      buf[j++] = escchar(str[i]);
    }
    else
      buf[j++] = str[i];
  }
  buf[j++] = '"';
  buf[j++] = '\0';

  repr = CCL_new_Str(buf);

  CCL_free(buf);

  return repr;
}

static CCL_Object *method_Str___str__(CCL_Object *me, int argc, CCL_Object **argv) {
  CCL_expect_argument_size(0, argc);
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
