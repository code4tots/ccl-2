#include "ccl_base.h"

#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int CCL_has_attribute(CCL_Object*, const char*);
CCL_Object *CCL_get_attribute(CCL_Object*, const char*);
void CCL_set_attribute(CCL_Object*, const char*, CCL_Object*);

CCL_Object *CCL_invoke_method(CCL_Object *me, const char *name, int argc, ...) {
  va_list ap;
  int i;
  size_t j;
  CCL_Implementation implementation = NULL;
  CCL_Object **argv, *result;

  for (j = 0; j < me->type->number_of_methods; j++) {
    if (strcmp(name, me->type->methods[j].name) == 0) {
      implementation = me->type->methods[j].implementation;
      break;
    }
  }

  if (implementation == NULL) {
    fprintf(stderr, "Object of type '%s' doesn't have method named '%s'\n", me->type->name, name);
    exit(1);
  }

  argv = malloc(sizeof(CCL_Object*));

  va_start(ap, argc);
  for (i = 0; i < argc; i++)
    argv[i] = va_arg(ap, CCL_Object*);
  va_end(ap);

  result = implementation(me, argc, argv);

  free(argv);

  return result;
}
