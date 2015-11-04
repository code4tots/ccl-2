#include "ccl.h"

CCL_Class CCL_s_Class_Nil = {
  "Nil",
  0, NULL, /* bases */
  0, NULL, /* direct attributes */
  0, NULL, /* direct methods */
  CCL_CLASS_TYPE_BUILTIN,
  NULL, /* builtin_constructor */
  NULL, /* instance */
  CCL_CLASS_EPILOGUE
};
extern CCL_Class CCL_s_Class_Bool;
extern CCL_Class CCL_s_Class_Num;
extern CCL_Class CCL_s_Class_Str;
extern CCL_Class CCL_s_Class_List;
extern CCL_Class CCL_s_Class_Dict;
extern CCL_Object CCL_s_nil = {CCL_Class_Nil, {NULL}};
extern CCL_Object CCL_s_true;
extern CCL_Object CCL_s_false;
