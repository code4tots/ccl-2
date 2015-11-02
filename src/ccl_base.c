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

  if (i == -1)
    CCL_err("Object of type '%s' doesn't have attribute named '%s'\n", me->type->name, name);

  return me->pointer_to.attributes[i];
}

void CCL_set_attribute(CCL_Object *me, const char *name, CCL_Object *value) {
  int i = get_index_of_attribute(me->type, name);

  if (i == -1)
    CCL_err("Object of type '%s' doesn't have attribute named '%s'\n", me->type->name, name);

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

  if (implementation == NULL)
    CCL_err("Object of type '%s' doesn't have method named '%s'\n", me->type->name, name);

  argv = malloc(sizeof(CCL_Object*));

  va_start(ap, argc);
  for (i = 0; i < argc; i++)
    argv[i] = va_arg(ap, CCL_Object*);
  va_end(ap);

  result = implementation(me, argc, argv);

  free(argv);

  return result;
}

CCL_Object *CCL_malloc_with_type(CCL_Type *type) {
  struct {
    CCL_Type *type;
    union {
      void *raw_data;
      CCL_Object **attributes;
    } pointer_to;
  } *me = malloc(sizeof(CCL_Object));
  me->type = type;
  return (CCL_Object*) me;
}

CCL_Object *CCL_new(CCL_Type *type, int argc, ...) {
  va_list ap;
  int i;
  CCL_Object **attributes, *result;

  if (!type->constructible)
    CCL_err("Tried to construct an object of type '%s', but '%s' is not constructible", type->name, type->name);

  if (type->number_of_attributes != argc)
    CCL_err("Type '%s' expects %d arguments in its constructor, but found %d arguments\n", type->name, type->number_of_attributes, argc);

  attributes = malloc(sizeof(CCL_Object*) * argc);

  va_start(ap, argc);
  for (i = 0; i < argc; i++)
    attributes[i] = va_arg(ap, CCL_Object*);
  va_end(ap);

  result = CCL_malloc_with_type(type);
  result->pointer_to.attributes = attributes;

  return result;
}

void CCL_err(const char *format, ...) {
  va_list ap;
  va_start(ap, format);
  vfprintf(stderr, format, ap);
  va_end(ap);
  exit(1);
}
