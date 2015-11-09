#ifndef CCL_CORE_H
#define CCL_CORE_H

#include <stdarg.h>
#include <stddef.h>

#define CCL_MAX_RECURSION_DEPTH 1000
#define CCL_CLASS_EPILOGUE NULL,0,0,NULL,0,NULL

enum CCL_CLASS_TYPE {
  CCL_CLASS_TYPE_BUILTIN,
  CCL_CLASS_TYPE_SINGLETON,
  CCL_CLASS_TYPE_DEFAULT
};

typedef enum CCL_CLASS_TYPE CCL_CLASS_TYPE;
typedef struct CCL_Class CCL_Class;
typedef struct CCL_Method CCL_Method;
typedef struct CCL_Object CCL_Object;
typedef struct CCL_StackFrame CCL_StackFrame;

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

  /* --- EPILOGUE: Fill in with CCL_CLASS_EPILOGUE in the initializer --- */

  /* Only meaningful if type == CCL_CLASS_TYPE_SINGLETON */
  CCL_Object *instance;

  /* Stuff filled in by CCL_initialize_class */
  int is_initialized;
  int number_of_ancestors;
  CCL_Class **ancestors; /* in MRO order */
  int number_of_attributes;
  const char **attribute_names;
};

struct CCL_Method {
  const char *const name;
  CCL_Object *(*const implementation)(CCL_Object*, int, CCL_Object**);
};

struct CCL_Object {
  CCL_Class *cls;
  union {
    void *raw_data;
    CCL_Object **attributes;
  } pointer_to;
};

struct CCL_StackFrame {
  CCL_Class *cls; /* class the method was called from */
  int ancestor_index; /* index into the ancestor that defines this method */
  int method_index; /* index into direct_methods of the ancestor with this method */
};

extern int CCL_recursion_depth;
extern CCL_StackFrame CCL_call_stack[CCL_MAX_RECURSION_DEPTH];

/* basic functions for public consumption */
CCL_Object *CCL_new(CCL_Class*, int, ...);
int CCL_has_attribute(CCL_Class*, const char*);
CCL_Object *CCL_set_attribute(CCL_Object*, const char*, CCL_Object*);
CCL_Object *CCL_get_attribute(CCL_Object*, const char*);
int CCL_has_method(CCL_Class*, const char*);
CCL_Object *CCL_invoke_method(CCL_Object*, const char*, int, ...);
void CCL_err(const char*, ...);

/* slightly more advanced usage functions */
void CCL_initialize_class(CCL_Class*);
const CCL_Method *CCL_find_next_method_and_source(CCL_Class*, const char*, int*, int*);
const CCL_Method *CCL_find_method(CCL_Class*, const char*);
CCL_Object *CCL_argv_new(CCL_Class*, int, CCL_Object**);
CCL_Object *CCL_argv_invoke_method(CCL_Object*, const char*, int, CCL_Object**);
int CCL_get_index_of_attribute(CCL_Class*, const char*);
void *CCL_malloc(size_t);
void *CCL_realloc(void*, size_t);
void CCL_free(void*);
CCL_Object *CCL_alloc(CCL_Class*);
CCL_Object *CCL_alloc_normal(CCL_Class *cls);
void CCL_print_stack_trace();
void CCL_vararg_err(const char *format, va_list ap);
void CCL_assert(int, const char*, ...);
void CCL_expect_argument_size(int, int);

#endif/*CCL_CORE_H*/
