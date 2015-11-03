#include "ccl.h"

#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define MAX_RECURSION_DEPTH 1000

static struct {
  const char *class_name, *method_name;
} stack_trace[MAX_RECURSION_DEPTH];

static int recursion_depth = 0;

static int get_index_of_attribute(CCL_Type *type, const char *name) {
  int i;

  for (i = 0; i < type->number_of_attributes; i++)
    if (strcmp(name, type->attribute_names[i]) == 0)
      return i;

  return -1;
}

static const CCL_Method *get_method(CCL_Type *type, const char *name) {
  int i;

  for (i = 0; i < type->number_of_methods; i++)
    if (strcmp(name, type->methods[i].name) == 0)
      return &type->methods[i];

  return NULL;
}

static CCL_Object *invoke_method(CCL_Object *me, const char *name, int argc, CCL_Object **argv) {
  int i;
  const CCL_Method *method = NULL;
  CCL_Implementation implementation = NULL;
  CCL_Object *result;

  method = get_method(me->type, name);

  if (method == NULL)
    CCL_err("Object of type '%s' doesn't have method named '%s'", me->type->name, name);

  stack_trace[recursion_depth].class_name = me->type->name;
  stack_trace[recursion_depth].method_name = method->name;
  recursion_depth++;

  if (recursion_depth >= MAX_RECURSION_DEPTH)
    CCL_err("Exceeded max recursion depth (max depth is set to: %d)", MAX_RECURSION_DEPTH);

  result = method->implementation(me, argc, argv);

  recursion_depth--;

  return result;
}

int CCL_has_method(CCL_Type *type, const char *name) {
  return get_method(type, name) != NULL;
}

int CCL_has_attribute(CCL_Object *me, const char *name) {
  return get_index_of_attribute(me->type, name) != -1;
}

CCL_Object *CCL_get_attribute(CCL_Object *me, const char *name) {
  int i = get_index_of_attribute(me->type, name);

  if (i == -1)
    CCL_err("Object of type '%s' doesn't have attribute named '%s'", me->type->name, name);

  return me->pointer_to.attributes[i];
}

void CCL_set_attribute(CCL_Object *me, const char *name, CCL_Object *value) {
  int i = get_index_of_attribute(me->type, name);

  if (i == -1)
    CCL_err("Object of type '%s' doesn't have attribute named '%s'", me->type->name, name);

  me->pointer_to.attributes[i] = value;
}

CCL_Object *CCL_invoke_method(CCL_Object *me, const char *name, int argc, ...) {
  va_list ap;
  int i;
  CCL_Implementation implementation = NULL;
  CCL_Object **argv, *result;

  argv = malloc(sizeof(CCL_Object*));

  va_start(ap, argc);
  for (i = 0; i < argc; i++)
    argv[i] = va_arg(ap, CCL_Object*);
  va_end(ap);

  result = invoke_method(me, name, argc, argv);

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
    CCL_err("Type '%s' expects %d arguments in its constructor, but found %d arguments", type->name, type->number_of_attributes, argc);

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
  int i;
  va_list ap;

  fprintf(stderr, "***** ERROR *****\n");

  va_start(ap, format);
  vfprintf(stderr, format, ap);
  va_end(ap);

  fprintf(stderr, "\n");

  for (i = recursion_depth-1; i >= 0; i--)
    fprintf(stderr, "in method %s#%s\n", stack_trace[i].class_name, stack_trace[i].method_name);

  exit(1);
}

void CCL_expect_number_of_arguments(int expected, int actual) {
  if (expected != actual)
    CCL_err("Expected %d argument(s), but found %d", expected, actual);
}

void CCL_expect_type_of_argument(CCL_Type *type, CCL_Object **argv, int index) {
  if (argv[index]->type != type)
    CCL_err("Expected argument %d to be of type '%s', but found type '%s'", index, type, argv[index]->type);
}

void CCL_expect_type_of_object(CCL_Type *type, CCL_Object *me) {
  if (me->type != type)
    CCL_err("Expected object to be of type '%s', but found '%s'", type, me->type);
}
