#ifndef CCL_STDLIB_H
#define CCL_STDLIB_H

#include "ccl_core.h"

#define CCL_Class_Object (&CCL_s_Class_Object)
#define CCL_Class_Nil (&CCL_s_Class_Nil)
#define CCL_Class_Bool (&CCL_s_Class_Bool)
#define CCL_Class_Num (&CCL_s_Class_Num)
#define CCL_Class_Str (&CCL_s_Class_Str)
#define CCL_Class_List (&CCL_s_Class_List)
#define CCL_Class_Dict (&CCL_s_Class_Dict)
#define CCL_nil (&CCL_s_nil)
#define CCL_true (&CCL_s_true)
#define CCL_false (&CCL_s_false)

extern CCL_Class CCL_s_Class_Object;
extern CCL_Class CCL_s_Class_Nil;
extern CCL_Class CCL_s_Class_Bool;
extern CCL_Class CCL_s_Class_Num;
extern CCL_Class CCL_s_Class_Str;
extern CCL_Class CCL_s_Class_List;
extern CCL_Class CCL_s_Class_Dict;
extern CCL_Object CCL_s_nil;
extern CCL_Object CCL_s_true;
extern CCL_Object CCL_s_false;

CCL_Object *CCL_new_Num(double);
CCL_Object *CCL_new_Str(const char*);
CCL_Object *CCL_new_List(int, ...);
CCL_Object *CCL_new_Dict(int, ...);

double CCL_Num_value(CCL_Object*);
const char *CCL_Str_value(CCL_Object*);
int CCL_Str_size(CCL_Object*);
CCL_Object *const *CCL_List_buffer(CCL_Object*);
int CCL_List_size(CCL_Object*);
int CCL_Dict_size(CCL_Object*);

#endif/*CCL_STDLIB_H*/
