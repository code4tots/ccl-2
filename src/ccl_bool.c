#include "ccl_bool.h"

static CCL_Object *method___str__(CCL_Object*, int, CCL_Object**);

static CCL_Method methods[] = {
  {"__str__", &method___str__}
};

CCL_Type CCL_Type_Bool_s = {"Bool", 0, NULL, sizeof(methods)/sizeof(CCL_Method), methods, 0};
CCL_Object CCL_true_s = {CCL_Type_Bool, {NULL}};
CCL_Object CCL_false_s = {CCL_Type_Bool, {NULL}};

static CCL_Object *method___str__(CCL_Object* me, int argc, CCL_Object** argv) {
  printf("Inside Bool#__str__");
  return NULL;
}
