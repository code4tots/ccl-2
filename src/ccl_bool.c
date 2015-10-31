#include "ccl_bool.h"

static CCL_Method methods[] = {
  {"__str__", NULL}
};
CCL_Type CCL_Type_Bool_s = {"Bool", 0, NULL, sizeof(methods)/sizeof(CCL_Method), methods};
CCL_Object CCL_true_s = {CCL_Type_Bool, NULL};
CCL_Object CCL_false_s = {CCL_Type_Bool, NULL};
