#include "ccl.h"

#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int CCL_recursion_depth = 0;
CCL_StackEntry CCL_stack_trace[CCL_MAX_RECURSION_DEPTH];

CCL_Object *CCL_new(CCL_Class *cls, int argc, ...) {
  va_list ap;
  CCL_Object **argv, *result;
  int i;

  argv = CCL_malloc(sizeof(CCL_Object*) * argc);
  va_start(ap, argc);
  for (i = 0; i < argc; i++)
    argv[i] = va_arg(ap, CCL_Object*);
  result = CCL_argv_new(cls, argc, argv);
  free(argv);

  return result;
}

int CCL_has_attribute(CCL_Class *cls, const char *name) {
  return CCL_get_index_of_attribute(cls, name) != -1;
}

void CCL_set_attribute(CCL_Object *me, const char *name, CCL_Object *value) {
  int i = CCL_get_index_of_attribute(me->cls, name);

  if (i == -1)
    CCL_err("Class '%s' has no attribute '%s'", me->cls->name, name);

  me->pointer_to.attributes[i] = value;
}

CCL_Object *CCL_get_attribute(CCL_Object *me, const char *name) {
  int i = CCL_get_index_of_attribute(me->cls, name);

  if (i == -1)
    CCL_err("Class '%s' has no attribute '%s'", me->cls->name, name);

  return me->pointer_to.attributes[i];
}

int CCL_has_method(CCL_Class *cls, const char *name) {
  return CCL_find_method(cls, name) != NULL;
}

CCL_Object *CCL_invoke_method(CCL_Object *me, const char *name, int argc, ...) {
  va_list ap;
  CCL_Object **argv, *result;
  int i;

  argv = CCL_malloc(sizeof(CCL_Object*) * argc);
  va_start(ap, argc);
  for (i = 0; i < argc; i++)
    argv[i] = va_arg(ap, CCL_Object*);
  result = CCL_argv_invoke_method(me, name, argc, argv);
  free(argv);

  return result;
}

void CCL_err(const char *format, ...) {
  va_list ap;

  fprintf(stderr, "***** ERROR *****\n");

  va_start(ap, format);
  vfprintf(stderr, format, ap);
  va_end(ap);

  fprintf(stderr, "\n");

  CCL_print_stack_trace();

  exit(1);
}

void CCL_initialize_class(CCL_Class *cls) {
  int i, j, k, n, found;
  CCL_Class **ancestors;
  const char **attribute_names;
  const CCL_Method **methods;

  if (cls->is_initialized)
    return;

  cls->is_initialized = 1;

  for (i = 0; i < cls->number_of_bases; i++)
    CCL_initialize_class(cls->bases[i]);

  /* For simplicity I use really dumb and inefficient algorithms
   * for filling in these attributes.
   * Maybe if I'm bored, I'll implement these more efficiently
   * in the future. */

  /* I'm using last occurance DFS for determing MRO.
   * Not sure if this is the same as C3, but this approach seems to
   * give the desired effect for the few examples off the top of
   * my head.
   * TODO: Investigate this. */

  /* Compute MRO */
  for (i = 0, n = 1; i < cls->number_of_bases; i++)
    n += cls->bases[i]->number_of_ancestors;

  ancestors = CCL_malloc(sizeof(CCL_Class*) * n);

  ancestors[0] = cls;
  for (i = 0, n = 1; i < cls->number_of_bases; i++)
    for (j = 0; j < cls->bases[i]->number_of_ancestors; j++)
      ancestors[n++] = cls->bases[i]->ancestors[j];

  for (i = n-1; i >= 0; i--)
    if (ancestors[i] != NULL)
      for (j = 0; j < i; j++)
        if (ancestors[j] == ancestors[i])
          ancestors[j] = NULL;

  for (k = i = 0; i < n; i++)
    if (ancestors[i] != NULL)
      ancestors[k++] = ancestors[i];

  cls->number_of_ancestors = k;
  cls->ancestors = CCL_realloc(ancestors, sizeof(CCL_Class*) * k);

  /* Compute attributes list */
  for (i = n = 0; i < cls->number_of_ancestors; i++)
    n += cls->ancestors[i]->number_of_direct_attributes;

  attribute_names = CCL_malloc(sizeof(char*) * n);

  for (i = n = 0; i < cls->number_of_ancestors; i++)
    for (j = 0; j < cls->ancestors[i]->number_of_direct_attributes; j++)
      attribute_names[n++] = cls->ancestors[i]->direct_attribute_names[j];

  /* TODO: check that there aren't any duplicate attribute names */
  cls->number_of_attributes = n;
  cls->attribute_names = attribute_names;

  /* Compute methods list */
  for (i = n = 0; i < cls->number_of_ancestors; i++)
    n += cls->ancestors[i]->number_of_direct_methods;

  methods = CCL_malloc(sizeof(CCL_Method*) * n);

  for (i = n = 0; i < cls->number_of_ancestors; i++)
    for (j = 0; j < cls->ancestors[i]->number_of_direct_methods; j++) {
      for (k = found = 0; k < n && !found; k++)
        if (strcmp(cls->ancestors[i]->direct_methods[j].name, methods[k]->name) == 0) {
          found = 1;
          break;
        }
      if (!found)
        methods[n++] = &cls->ancestors[i]->direct_methods[j];
    }

  cls->number_of_methods = n;
  cls->methods = methods; /* ~ breaks const correctness */
}

const CCL_Method *CCL_find_direct_method(CCL_Class *cls, const char *name) {
  int i;

  for (i = 0; i < cls->number_of_direct_methods; i++)
    if (strcmp(name, cls->direct_methods[i].name) == 0)
      return &cls->direct_methods[i];

  return NULL;
}

const CCL_Method *CCL_find_method_and_class(CCL_Class *cls, const char *name, CCL_Class **pointer_to_owner_class) {
  int i;

  for (i = 0; i < cls->number_of_ancestors; i++) {
    const CCL_Method *method = CCL_find_direct_method(cls->ancestors[i], name);
    if (method != NULL) {
      *pointer_to_owner_class = cls->ancestors[i];
      return method;
    }
  }

  return NULL;
}
const CCL_Method *CCL_find_method(CCL_Class *cls, const char *name) {
  CCL_Class *owner_class;
  return CCL_find_method_and_class(cls, name, &owner_class);
}

CCL_Object *CCL_argv_new(CCL_Class *cls, int argc, CCL_Object **argv) {
  int i;
  CCL_Object *me;

  CCL_initialize_class(cls);

  switch(cls->type) {
  case CCL_CLASS_TYPE_BUILTIN:
    if (cls->builtin_constructor == NULL)
      CCL_err("Builtin class '%s' is not constructible", cls->name);
    return cls->builtin_constructor(argc, argv);
  case CCL_CLASS_TYPE_SINGLETON:
    if ((me = cls->instance) != NULL)
      break;
  case CCL_CLASS_TYPE_DEFAULT:
    me = CCL_malloc(sizeof(CCL_Object));
    me->cls = cls;
    me->pointer_to.attributes = CCL_malloc(sizeof(CCL_Object*) * cls->number_of_attributes);
    for (i = 0; i < cls->number_of_attributes; i++)
      me->pointer_to.attributes[i] = CCL_nil;
    break;
  default:
    CCL_err("Class '%s' has invalid type: %d", cls->name, cls->type);
  }

  CCL_argv_invoke_method(me, "__init__", argc, argv);

  return me;
}

CCL_Object *CCL_argv_invoke_method(CCL_Object *me, const char *name, int argc, CCL_Object **argv) {
  CCL_Class *source_class;
  CCL_Object *result;
  const CCL_Method *method = CCL_find_method_and_class(me->cls, name, &source_class);

  if (method == NULL)
    CCL_err("Class '%s' has no method '%s'", me->cls->name, name);

  CCL_stack_trace[CCL_recursion_depth].class_name = me->cls->name;
  CCL_stack_trace[CCL_recursion_depth].source_class_name = source_class->name;
  CCL_stack_trace[CCL_recursion_depth].method_name = name;
  CCL_recursion_depth++;
  result = method->implementation(me, argc, argv);
  CCL_recursion_depth--;

  return result;
}

int CCL_get_index_of_attribute(CCL_Class *cls, const char *name) {
  int i;

  for (i = 0; i < cls->number_of_attributes; i++)
    if (strcmp(name, cls->attribute_names[i]) == 0)
      return i;

  return -1;
}

void *CCL_malloc(size_t size) {
  void *ptr = malloc(size);
  /* Pray that fprintf doesn't call malloc/realloc.
   * Unfortunately, this is not something you can rely on 100%.
   * TODO: implement better means of detecting and reporting OOM.
   */
  if (ptr == NULL)
    CCL_err("Out of memory in 'malloc'");
  return ptr;
}

void *CCL_realloc(void *ptr, size_t size) {
  ptr = realloc(ptr, size);
  /* Pray that fprintf doesn't call malloc/realloc.
   * Unfortunately, this is not something you can rely on 100%.
   * TODO: implement better means of detecting and reporting OOM.
   */
  if (ptr == NULL)
    CCL_err("Out of memory in 'realloc'");
  return ptr;
}

void CCL_print_stack_trace() {
  int i;

  for (i = CCL_recursion_depth-1; i >= 0; i--)
    fprintf(stderr, "  in method %s->%s#%s\n",
            CCL_stack_trace[i].class_name,
            CCL_stack_trace[i].source_class_name,
            CCL_stack_trace[i].method_name);
}
