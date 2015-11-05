#include "ccl.h"

static CCL_Class *bases_Nil[] = {
  CCL_Class_Object
};

CCL_Class CCL_s_Class_Object = {
  "Object",
  0, NULL, /* bases */
  0, NULL, /* direct attributes */
  0, NULL, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Class CCL_s_Class_Nil = {
  "Nil",
  sizeof(bases_Nil)/sizeof(CCL_Class), bases_Nil,
  0, NULL, /* direct attributes */
  0, NULL, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Class CCL_s_Class_Bool = {
  "Bool",
  sizeof(bases_Nil)/sizeof(CCL_Class), bases_Nil,
  0, NULL, /* direct attributes */
  0, NULL, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Class CCL_s_Class_Num = {
  "Num",
  sizeof(bases_Nil)/sizeof(CCL_Class), bases_Nil,
  0, NULL, /* direct attributes */
  0, NULL, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Class CCL_s_Class_Str = {
  "Str",
  sizeof(bases_Nil)/sizeof(CCL_Class), bases_Nil,
  0, NULL, /* direct attributes */
  0, NULL, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Class CCL_s_Class_List = {
  "List",
  sizeof(bases_Nil)/sizeof(CCL_Class), bases_Nil,
  0, NULL, /* direct attributes */
  0, NULL, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Class CCL_s_Class_Dict = {
  "Dict",
  sizeof(bases_Nil)/sizeof(CCL_Class), bases_Nil,
  0, NULL, /* direct attributes */
  0, NULL, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  CCL_CLASS_EPILOGUE
};

CCL_Object CCL_s_nil = {CCL_Class_Nil, {NULL}};
CCL_Object CCL_s_true = {CCL_Class_Bool, {NULL}};
CCL_Object CCL_s_false = {CCL_Class_Bool, {NULL}};;

CCL_Object *CCL_new_Num(double value) {
}
CCL_Object *CCL_new_Str(const char*);
CCL_Object *CCL_new_List(int, ...);
CCL_Object *CCL_new_Dict(int, ...);

double CCL_Num_value(CCL_Object*);
const char *CCL_Str_value(CCL_Object*);
int CCL_Str_size(CCL_Object*);
CCL_Object *const *CCL_List_buffer(CCL_Object*);
int CCL_List_size(CCL_Object*);
int CCL_Dict_size(CCL_Object*);
