#include "ccl.h"

#include <stdio.h>

static CCL_Object *method___str__(CCL_Object*, int, CCL_Object**);

static CCL_Method methods[] = {
  {"__str__", &method___str__}
};
CCL_Type CCL_Type_Nil_s = {"Nil", 0, NULL, sizeof(methods)/sizeof(CCL_Method), methods, 0};
CCL_Object CCL_nil_s = {CCL_Type_Nil, {NULL}};

static CCL_Object *method___str__(CCL_Object* me, int argc, CCL_Object** argv) {
  CCL_expect_number_of_arguments(0, argc);
  printf("Inside Nil#__str__\n");
  return NULL;
}
