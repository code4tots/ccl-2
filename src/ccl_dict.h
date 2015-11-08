#ifndef CCL_DICT_H
#define CCL_DICT_H

#include "ccl_core.h"

#define CCL_Class_Dict (&CCL_s_Class_Dict)

extern CCL_Class CCL_s_Class_Dict;

CCL_Object *CCL_new_Dict(int, ...);
int CCL_Dict_size(CCL_Object*);

#endif/*CCL_DICT_H*/
