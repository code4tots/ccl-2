#ifndef CCL_BASE_H
#define CCL_BASE_H
#include <stddef.h>

typedef struct CCL_Method CCL_Method;
typedef struct CCL_Type CCL_Type;
typedef struct CCL_Object CCL_Object;
typedef CCL_Object *(*CCL_Implementation)(CCL_Object*, int, CCL_Object**);

struct CCL_Method {
  const char *name;
  CCL_Implementation implementation;
};

struct CCL_Type {
  const char *name;
  size_t number_of_attributes;
  const char **attribute_names;
  size_t number_of_methods;
  CCL_Method *methods;
};

struct CCL_Object {
  CCL_Type *type;
  union {
    void *raw_data;
    CCL_Object **attributes;
  } pointer_to;
};

int CCL_has_attribute(CCL_Object*, const char*);
CCL_Object *CCL_get_attribute(CCL_Object*, const char*);
void CCL_set_attribute(CCL_Object*, const char*, CCL_Object*);
CCL_Object *CCL_invoke_method(CCL_Object*, const char*, int, ...);

#endif/*CCL_BASE_H*/
