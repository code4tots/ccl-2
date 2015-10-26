#include <stddef.h>

enum CCL_Type {
  CCL_NIL,
  CCL_BOOL,
  CCL_NUM,
  CCL_STR,
  CCL_LIST,
  CCL_DICT,
  CCL_FUNC,
  CCL_LAMBDA,
  CCL_MACRO,
  CCL_USER_OBJECT
};

enum CCL_Bool {
  CCL_FASE = 0,
  CCL_TRUE = 1
};

typedef enum CCL_Type CCL_Type;
typedef enum CCL_Bool CCL_Bool;
typedef enum CCL_Macro CCL_Macro;
typedef struct CCL_str CCL_str;
typedef struct CCL_list CCL_list;
typedef struct CCL_dict CCL_dict;
typedef struct CCL_func CCL_func;
typedef struct CCL_lambda CCL_lambda;
typedef struct CCL_macro CCL_macro;
typedef struct CCL_user_object CCL_user_object;
typedef struct CCL_Object CCL_Object;

struct CCL_str {
  size_t size;
  char *buffer;
};

struct CCL_list {
  size_t size, capacity;
  CCL_Object **buffer;
};

struct CCL_dict {
  CCL_Object *key, *value;
  CCL_dict *parent, *children[2];
  size_t size;
};

struct CCL_func {
  CCL_Object *name, *(*func)(CCL_Object *arguments);
};

struct CCL_lambda {
  CCL_Object *context, *argument_names, *body;
};

struct CCL_macro {
  CCL_Object *name, *(*func)(CCL_Object *context, CCL_Object *raw_arguments);
};

struct CCL_user_object {
  CCL_Object *metadict, *attributes;
};

struct CCL_Object {
  CCL_Type type;
  union {
    CCL_Bool as_bool;
    double as_num;
    CCL_str as_str;
    CCL_list as_list;
    CCL_dict *as_dict;
    CCL_func as_func;
    CCL_lambda as_lambda;
    CCL_macro as_macro;
    CCL_user_object as_user_object;
  } value;
};

/* Initializes CCL. Must be called before any other CCL function. */
void CCL_init();

/* Initializes the memory pool. Any object created before this is called
 * will not be gc'd. */
extern CCL_Object *CCL_memory_pool;
void CCL_init_memory_pool();
void CCL_free_memory_pool();
void CCL_garbage_collect_memory_pool(CCL_Object *context);
/* TODO: incremental gc */

/* Means of creating new CCL objects. */
extern CCL_Object *CCL_nil;
extern CCL_Object *CCL_false;
extern CCL_Object *CCL_true;
CCL_Object *CCL_new_num(double value);
CCL_Object *CCL_new_str(const char *value);
CCL_Object *CCL_new_substr(const char *value, size_t length);
CCL_Object *CCL_new_list(int, ...);
CCL_Object *CCL_new_dict(int, ...);
CCL_Object *CCL_new_func(const char *name, CCL_Object *(*func)(CCL_Object*));
CCL_Object *CCL_new_lambda(CCL_Object *context, CCL_Object *argument_names, CCL_Object *body);
CCL_Object *CCL_new_macro(const char *name, CCL_Object *(*func)(CCL_Object*,CCL_Object*));
CCL_Object *CCL_new_user_object(CCL_Object *metadict);

/* Manually delete objects. */
CCL_Object *CCL_free(CCL_Object*);

/* Useful primitive functions on CCL objects. */
CCL_Object *CCL_strcat(CCL_Object *list_of_strs);
void CCL_list_add(CCL_Object *list, CCL_Object *item);
CCL_Object *CCL_list_pop(CCL_Object *list);
CCL_Object *CCL_dict_set(CCL_Object *dict, CCL_Object *key, CCL_Object *value);
CCL_Object *CCL_dict_get(CCL_Object *dict, CCL_Object *key);
size_t CCL_dict_size(CCL_Object *dict);
CCL_dict *CCL_dict_next(CCL_dict *node);
CCL_dict *CCL_dict_first(CCL_dict *node);
CCL_Object *CCL_user_object_invoke_method(CCL_Object *user_object, CCL_Object *method_name, CCL_Object *arguments);

/* Language infrastructure */
extern CCL_Object *CCL_root_context;
CCL_Object *CCL_context_lookup(CCL_Object *context, CCL_Object *key);
CCL_Object *CCL_context_assign(CCL_Object *context, CCL_Object *key, CCL_Object *value);
CCL_Object *CCL_context_declare(CCL_Object *context, CCL_Object *key, CCL_Object *value);
CCL_Object *CCL_eval(CCL_Object *context, CCL_Object *node);
