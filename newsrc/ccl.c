#include "ccl.h"

struct CCL_State {
  int error;
  CCL_Object* nil;
  CCL_Object* true_;
  CCL_Object* false_;
  CCL_Object* Class;
  CCL_Object* Nil;
  CCL_Object* Bool;
  CCL_Object* Number;
  CCL_Object* String;
  CCL_Object* List;
  CCL_Object* Table;
};

struct CCL_Class {
  char* name;
  int is_native;

  int number_of_bases;
  CCL_Class** bases;

  int number_of_attributes;
  char** attributes;

  int number_of_normal_methods;
  char** normal_method_names;
  CCL_Object** normal_methods;

  int number_of_native_methods;
  char** native_method_names;
  CCL_Object* (**native_methods)(
      CCL_State* state,
      CCL_Object* me,
      int argc, CCL_Object** argv);
};
