typedef struct CCL_State CCL_State;
typedef struct CCL_Object CCL_Object;
typedef struct CCL_Class CCL_Class;

struct CCL_Object {
  CCL_Object* cls;
  void* data;
  CCL_Object** attrs;
};

CCL_State *CCL_init();

CCL_Object* CCL_argv_new_Class(
    CCL_State* state,
    const char* name,
    int is_native,
    int number_of_bases,
    CCL_Object** bases,
    int number_of_attributes,
    const char** attribute_names,
    int number_of_normal_methods,
    const char** normal_method_names,
    CCL_Object** normal_methods,
    int number_of_native_methods,
    const char** native_method_names,
    CCL_Object* (**native_methods)(
        CCL_State* state,
        CCL_Object* me,
        int argc, CCL_Object** argv));

CCL_Object* CCL_new_Class(
    CCL_State* state,
    const char* name,
    int is_native,
    int number_of_bases,
    int number_of_attributes,
    int number_of_normal_methods,
    int number_of_native_methods,
    ...);

CCL_Object* CCL_new_Nil(CCL_State* state);
CCL_Object* CCL_new_Bool(CCL_State* state, int value);
CCL_Object* CCL_new_Number(CCL_State* state, double value);
CCL_Object* CCL_new_String(CCL_State* state, const char* value);
CCL_Object* CCL_argv_new_List(CCL_State* state, int argc, CCL_Object** argv);
CCL_Object* CCL_new_List(CCL_State* state, int argc, ...);
CCL_Object* CCL_argv_new_Table(CCL_State* state, int argc, CCL_Object** argv);
CCL_Object* CCL_new_Table(CCL_State* state, int argc, ...);
CCL_Object* CCL_argv_new(
    CCL_State* state, CCL_Object* cls, int argc, CCL_Object** argv);
CCL_Object* CCL_new(CCL_State* state, CCL_Object* cls, int argc, ...);

int CCL_hasmethod(CCL_State* state, CCL_Object* me, const char* name);
CCL_Object* CCL_argv_callmethod(
    CCL_State* state,
    CCL_Object* me,
    const char* name,
    int number_of_arguments,
    CCL_Object** arguments);
CCL_Object* CCL_callmethod(
    CCL_State* state,
    CCL_Object* me,
    const char* name,
    int number_of_arguments,
    ...);

int CCL_hasattr(CCL_State* state, CCL_Object* me, const char* name);
CCL_Object* CCL_getattr(CCL_State* state, CCL_Object* me, const char* name);
CCL_Object* CCL_setattr(
    CCL_State* state, CCL_Object* me, const char* name, CCL_Object* value);
