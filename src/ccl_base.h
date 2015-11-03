#ifndef CCL_BASE_H
#define CCL_BASE_H
#include <stddef.h>

typedef struct CCL_Method CCL_Method;
typedef struct CCL_Type CCL_Type;
typedef struct CCL_Object CCL_Object;
typedef CCL_Object *(*CCL_Implementation)(CCL_Object*, int, CCL_Object**);

struct CCL_Method {
  const char *const name;
  const CCL_Implementation implementation;
};

struct CCL_Type {
  const char *const name;
  const int number_of_ancestors;
  CCL_Type *const *const ancestors; /* in mro order */
  const int number_of_attributes; /* number of direct attributes */
  const char *const *const attribute_names;
  const int number_of_methods; /* number of direct methods */
  const CCL_Method *const methods;
  const int constructible; /* false for e.g. builtins and singletons */
};

struct CCL_Object {
  CCL_Type *const type;
  union {
    void *raw_data;
    CCL_Object **attributes;
  } pointer_to;
};

int CCL_has_method(CCL_Type*, const char*);
int CCL_has_attribute(CCL_Object*, const char*);
CCL_Object *CCL_get_attribute(CCL_Object*, const char*);
void CCL_set_attribute(CCL_Object*, const char*, CCL_Object*);
CCL_Object *CCL_invoke_method(CCL_Object*, const char*, int, ...);
CCL_Object *CCL_malloc_with_type(CCL_Type*);
CCL_Object *CCL_new(CCL_Type*, int, ...);
void CCL_err(const char*, ...);
void CCL_expect_number_of_arguments(int expected, int actual);
void CCL_expect_type_of_argument(CCL_Type*, CCL_Object**, int);
void CCL_expect_type_of_object(CCL_Type*, CCL_Object*);

#endif/*CCL_BASE_H*/
