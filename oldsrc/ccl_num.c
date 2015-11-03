#include "ccl.h"

#include <stdio.h>
#include <stdlib.h>

#define NUM_DATA(me) (*(double*)(me->pointer_to.raw_data))

static CCL_Object *method___str__(CCL_Object*, int, CCL_Object**);
static CCL_Object *method___add__(CCL_Object*, int, CCL_Object**);

static CCL_Method methods[] = {
  {"__add__", &method___add__},
  {"__str__", &method___str__}
};
CCL_Type CCL_s_Type_Num = {"Num", 0, NULL, sizeof(methods)/sizeof(CCL_Method), methods, 0};

static CCL_Object *method___str__(CCL_Object* me, int argc, CCL_Object** argv) {
  printf("Inside Num#__str__\n");
  CCL_expect_number_of_arguments(0, argc);
  return NULL;
}

static CCL_Object *method___add__(CCL_Object *me, int argc, CCL_Object **argv) {
  CCL_expect_number_of_arguments(1, argc);
  CCL_expect_type_of_argument(CCL_Type_Num, argv, 0);
  return CCL_new_Num(NUM_DATA(me) + NUM_DATA(argv[0]));
}

CCL_Object *CCL_new_Num(double value) {
  CCL_Object *me = CCL_malloc_with_type(CCL_Type_Num);
  me->pointer_to.raw_data = malloc(sizeof(double));
  *((double*) me->pointer_to.raw_data) = value;
  return me;
}

double CCL_as_Num(CCL_Object *me) {
  CCL_expect_type_of_object(CCL_Type_Num, me);
  return NUM_DATA(me);
}
