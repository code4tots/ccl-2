#include "ccl.h"

#include <stdarg.h>
#include <stdlib.h>
#include <string.h>

typedef struct Object Object;

int CCL_recursion_depth = 0;
CCL_StackEntry CCL_stack_trace[CCL_MAX_RECURSION_DEPTH];

/* Object should mirror CCL_Object except that pointer to cls is not const. */
struct Object {
  CCL_Class *cls;
  union {
    void *raw_data;
    CCL_Object **attributes;
  } pointer_to;
};

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
  int i, j, k, n;
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
  for (i = n = 0; i < cls->number_of_bases; i++)
    n += cls->bases[i]->number_of_ancestors;

  ancestors = CCL_malloc(sizeof(CCL_Class*) * n);

  for (i = k = 0; i < cls->number_of_bases; i++)
    for (j = 0; j < cls->bases[i]->number_of_ancestors; j++)
      ancestors[k++] = cls->bases[i]->ancestors[j];

  for (i = n-1; i >= 0; i--)
    if (ancestors[i] != NULL)
      for (j = i-1; j >= 0; j--)
        if (ancestors[j] == ancestors[i])
          ancestors[j] = NULL;

  for (n = i = 0; i < n; i++)
    if (ancestors[i] != NULL)
      ancestors[n++] = ancestors[i];

  cls->number_of_ancestors = n;
  cls->ancestors = CCL_realloc(ancestors, sizeof(CCL_Class*) * n);

  /* Compute attributes list */
  n = cls->number_of_direct_attributes;
  for (i = 0; i < cls->number_of_bases; i++)
    n += cls->bases[i]->number_of_attributes;

  attribute_names = CCL_malloc(sizeof(char*) * n);

  for (n = 0; n < cls->number_of_direct_attributes; n++)
    attribute_names[n] = cls->direct_attribute_names[n];

  for (i = 0; i < cls->number_of_bases; i++)
    for (j = 0; j < cls->bases[i]->number_of_attributes; j++) {
      int found = 0;
      for (k = 0; k < n; k++)
        if (strcmp(cls->bases[i]->attribute_names[j], attribute_names[k]) == 0) {
          found = 1;
          break;
        }
      if (!found)
        attribute_names[n++] = cls->bases[i]->attribute_names[j];
    }

  cls->number_of_attributes = n;
  cls->attribute_names = CCL_realloc(attribute_names, sizeof(char*) * n);

  /* Compute methods list */
  n = cls->number_of_direct_methods;
  for (i = 0; i < cls->number_of_ancestors; i++)
    n += cls->ancestors[i]->number_of_ancestors;

  methods = CCL_malloc(sizeof(CCL_Method*) * n);

  for (n = 0; n < cls->number_of_direct_methods; n++)
    methods[n] = &cls->direct_methods[n];

  for (i = n = 0; i < cls->number_of_ancestors; i++)
    for (j = 0; j < cls->ancestors[i]->number_of_direct_methods; j++) {
      int found = 0;
      for (k = 0; k < n; k++)
        if (strcmp(cls->ancestors[i]->direct_methods[j].name, methods[k]->name) == 0) {
          found = 1;
          break;
        }
      if (!found)
        methods[n++] = &cls->ancestors[i]->direct_methods[j];
    }

    cls->number_of_methods = n;
    cls->methods = CCL_realloc(methods, sizeof(CCL_Method*) * n);
}

CCL_Object *CCL_argv_new(CCL_Class *cls, int argc, CCL_Object **argv) {
  /* TODO */
  return NULL;
}

int CCL_get_index_of_attribute(CCL_Class *cls, const char *name) {
  int i;

  for (i = 0; i < cls->number_of_attributes; i++)
    if (strcmp(name, cls->attribute_names[i]) == 0)
      return i;

  return -1;
}

CCL_Object *CCL_allocate_memory_for_object_of_class(CCL_Class *cls) {
  Object *me = CCL_malloc(sizeof(CCL_Object*));
  me->cls = cls;
  return (CCL_Object*) me;
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
    fprintf(stderr, "  in method %s->%s#%s (%d)\n",
            CCL_stack_trace[i].class_name,
            CCL_stack_trace[i].source_class_name,
            CCL_stack_trace[i].method_name,
            CCL_stack_trace[i].tag);
}
