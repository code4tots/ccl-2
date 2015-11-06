#ifndef CCL_STR_H
#define CCL_STR_H

#include "ccl_core.h"

#define CCL_Class_Str (&CCL_s_Class_Str)

extern CCL_Class CCL_s_Class_Str;

CCL_Object *CCL_new_Str(const char*);
const char *CCL_Str_buffer(CCL_Object*);
int CCL_Str_size(CCL_Object*);

#endif/*CCL_STR_H*/
