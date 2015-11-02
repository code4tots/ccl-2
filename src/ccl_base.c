#include "ccl_base.h"

#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int get_index_of_attribute(CCL_Type *type, const char *name) {
  int i;

  for (i = 0; i < type->number_of_attributes; i++)
    if (strcmp(name, type->attribute_names[i]) == 0)
      return i;

  return -1;
}

int CCL_has_attribute(CCL_Object *me, const char *name) {
  return get_index_of_attribute(me->type, name) != -1;
}

CCL_Object *CCL_get_attribute(CCL_Object *me, const char *name) {
  int i = get_index_of_attribute(me->type, name);

  if (i == -1) {
    fprintf(stderr, "Object of type '%s' does not have attribute '%s'\n", me->type->name, name);
    exit(1);
  }

  return me->pointer_to.attributes[i];
}

void CCL_set_attribute(CCL_Object *me, const char *name, CCL_Object *value) {
  int i = get_index_of_attribute(me->type, name);

  if (i == -1) {
    fprintf(stderr, "Object of type '%s' does not have attribute '%s'\n", me->type->name, name);
    exit(1);
  }

  me->pointer_to.attributes[i] = value;
}

CCL_Object *CCL_invoke_method(CCL_Object *me, const char *name, int argc, ...) {
  va_list ap;
  int i;
  CCL_Implementation implementation = NULL;
  CCL_Object **argv, *result;

  for (i = 0; i < me->type->number_of_methods; i++) {
    if (strcmp(name, me->type->methods[i].name) == 0) {
      implementation = me->type->methods[i].implementation;
      break;
    }
  }

  if (implementation == NULL) {
    fprintf(stderr, "Object of type '%s' doesn't have method '%s'\n", me->type->name, name);
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
