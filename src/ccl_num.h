#ifndef CCL_NUM_H
#define CCL_NUM_H
#include "ccl_base.h"

#define CCL_Type_Num (&CCL_s_Type_Num)

extern CCL_Type CCL_s_Type_Num;

CCL_Object *CCL_new_Num(double value);
double CCL_as_Num(CCL_Object*);

#endif/*CCL_NUM_H*/
