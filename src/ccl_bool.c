#include "ccl_bool.h"

#include <stdio.h>

static CCL_Object *method___str__(CCL_Object*, int, CCL_Object**);

static CCL_Method methods[] = {
  {"__str__", &method___str__}
};

CCL_Type CCL_s_Type_Bool = {"Bool", 0, NULL, sizeof(methods)/sizeof(CCL_Method), methods, 0};
CCL_Object CCL_s_true = {CCL_Type_Bool, {NULL}};
CCL_Object CCL_s_false = {CCL_Type_Bool, {NULL}};

static CCL_Object *method___str__(CCL_Object* me, int argc, CCL_Object** argv) {
  CCL_expect_number_of_arguments(0, argc);
  printf("Inside Bool#__str__");
  return NULL;
}
