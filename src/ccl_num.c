#include "ccl.h"

#include <stdio.h>
#include <stdlib.h>

static CCL_Object *method___str__(CCL_Object*, int, CCL_Object**);
static CCL_Object *method___Add__(CCL_Object*, int, CCL_Object**);

static CCL_Method methods[] = {
  {"__Add__", &method___Add__},
  {"__str__", &method___str__}
};
CCL_Type CCL_Type_Num_s = {"Num", 0, NULL, sizeof(methods)/sizeof(CCL_Method), methods, 0};

static CCL_Object *method___str__(CCL_Object* me, int argc, CCL_Object** argv) {
  printf("Inside Num#__str__\n");
  return NULL;
}

static CCL_Object *method___Add__(CCL_Object *me, int argc, CCL_Object **argv) {
  return NULL;
}

CCL_Object *CCL_new_Num(double value) {
  CCL_Object *me = CCL_malloc_with_type(CCL_Type_Num);
  me->pointer_to.raw_data = malloc(sizeof(double));
  *((double*) me->pointer_to.raw_data) = value;
  return me;
}
