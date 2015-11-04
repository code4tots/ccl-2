#ifndef CCL_CORE_H
#define CCL_CORE_H

#include <stddef.h>

#define CCL_MAX_RECURSION_DEPTH 1000
#define CCL_CLASS_EPILOGUE 0,0,NULL,0,NULL,0,NULL

enum CCL_CLASS_TYPE {
  CCL_CLASS_TYPE_BUILTIN,
  CCL_CLASS_TYPE_SINGLETON,
  CCL_CLASS_TYPE_DEFAULT
};

typedef enum CCL_CLASS_TYPE CCL_CLASS_TYPE;
typedef struct CCL_Class CCL_Class;
typedef struct CCL_Method CCL_Method;
typedef struct CCL_Object CCL_Object;
typedef struct CCL_StackEntry CCL_StackEntry;

struct CCL_Class {
  /* Stuff filled in by the user/transpiler */
  const char *const name;
  const int number_of_bases;
  CCL_Class *const *const bases;
  const int number_of_direct_attributes;
  const char *const *const direct_attribute_names;
  const int number_of_direct_methods;
  const CCL_Method *const direct_methods;
  const CCL_CLASS_TYPE type;

  /* Only meaningful if type == CCL_CLASS_TYPE_BUILTIN */
  CCL_Object *(*const builtin_constructor)(int, CCL_Object**);

  /* Only meaningful if type == CCL_CLASS_TYPE_SINGLETON */
  CCL_Object *instance;

  /* Stuff filled in by CCL_initialize_class */
  int is_initialized;
  int number_of_ancestors;
  CCL_Class **ancestors; /* in MRO order */
  int number_of_attributes;
  const char **attribute_names;
  int number_of_methods;
  const CCL_Method **methods;
};

struct CCL_Method {
  const char *const name;
  CCL_Object *(*const implementation)(CCL_Object*, int, CCL_Object**);
};

struct CCL_Object {
  CCL_Class *const cls;
  union {
    void *raw_data;
    CCL_Object **attributes;
  } pointer_to;
};

struct CCL_StackEntry {
  const char *class_name; /* name of class the method was called from */
  const char *source_class_name; /* name of class the source of this code lives in */
  const char *method_name; /* name of the method that was called */
};

extern int CCL_recursion_depth;
extern CCL_StackEntry CCL_stack_trace[CCL_MAX_RECURSION_DEPTH];

/* basic functions for public consumption */
CCL_Object *CCL_new(CCL_Class*, int, ...);
int CCL_has_attribute(CCL_Class*, const char*);
void CCL_set_attribute(CCL_Object*, const char*, CCL_Object*);
CCL_Object *CCL_get_attribute(CCL_Object*, const char*);
int CCL_has_method(CCL_Class*, const char*);
CCL_Object *CCL_invoke_method(CCL_Object*, const char*, int, ...);
void CCL_err(const char*, ...);

/* slightly more advanced usage functions */
void CCL_initialize_class(CCL_Class*);
const CCL_Method *CCL_find_direct_method(CCL_Class*, const char*);
const CCL_Method *CCL_find_method_and_class(CCL_Class*, const char*, CCL_Class**);
const CCL_Method *CCL_find_method(CCL_Class*, const char*);
CCL_Object *CCL_argv_new(CCL_Class*, int, CCL_Object**);
CCL_Object *CCL_argv_invoke_method(CCL_Object*, const char*, int, CCL_Object**);
int CCL_get_index_of_attribute(CCL_Class*, const char*);
CCL_Object *CCL_allocate_memory_for_object_of_class(CCL_Class*);
void *CCL_malloc(size_t);
void *CCL_realloc(void*, size_t);
void CCL_print_stack_trace();

#endif/*CCL_CORE_H*/
