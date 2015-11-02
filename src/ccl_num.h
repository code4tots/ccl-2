#ifndef CCL_NUM_H
#define CCL_NUM_H
#include "ccl_base.h"

#define CCL_Type_Num (&CCL_Type_Num_s)

extern CCL_Type CCL_Type_Num_s;

CCL_Object *CCL_new_Num(double value);

#endif/*CCL_NUM_H*/
