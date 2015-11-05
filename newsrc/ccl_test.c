/*
Windows: cl /Za /Feccl_test *.c && ccl_test
OS X: gcc -std=c89 -pedantic -Wall -Wmissing-braces -Wextra -Wmissing-field-initializers -Wformat=2 -Wswitch-default -Wswitch-enum -Wcast-align -Wpointer-arith -Wbad-function-cast -Wstrict-overflow=5 -Wstrict-prototypes -Winline -Wundef -Wnested-externs -Wcast-qual -Wshadow -Wunreachable-code -Wfloat-equal -Wstrict-aliasing=1 -Wredundant-decls -Wold-style-definition -Werror -ggdb3 -O0 -fno-omit-frame-pointer -fno-common -fstrict-aliasing -Wno-unused-parameter -Wno-format-nonliteral -lm *.c
*/
#include "ccl.h"

#include <stdio.h>
#include <string.h>

void test() {
  CCL_Object *x, *y;

  CCL_assert(
      CCL_Num_value(CCL_new_Num(5)) == 5,
      "Expected CCL_Num_value(CCL_new_Num(5)) == 5");

  x = CCL_new_Str("Hello world!");

  CCL_assert(
      CCL_Str_size(x) == 12,
      "Expected CCL_Str_size(x) == 12");

  CCL_assert(
      strcmp("Hello world!", CCL_Str_buffer(x)) == 0,
      "Expected strcmp(\"Hello world!\", CCL_Str_buffer(x)) == 0, but "
      "CCL_Str_buffer(x) == \"%s\"", CCL_Str_buffer(x));

  x = CCL_new_Num(5);

  CCL_assert(CCL_Class_Object->is_initialized, "Expected Object to be initialized");
  CCL_assert(CCL_Class_Num->is_initialized, "Expected Num to be initialized");
  CCL_assert(CCL_Class_Num->number_of_ancestors == 2, "Expected Num to have exactly 2 ancestors");
  CCL_assert(CCL_has_method(CCL_Class_Num, "__repr__"), "Expected Num to have method '__repr__'");
  CCL_assert(CCL_has_method(CCL_Class_Object, "__str__"), "Expected Object to have method '__str__'");
  CCL_assert(CCL_has_method(CCL_Class_Num, "__str__"), "Expected Num to have method '__str__'");

  y = CCL_invoke_method(x, "__str__", 0);

  CCL_assert(y->cls == CCL_Class_Str, "Expected y->cls == CCL_Class_Str");

  CCL_assert(
      strcmp("5", CCL_Str_buffer(y)) == 0,
      "Expected strcmp(\"5\", CCL_Str_buffer(y)) == 0, but "
      "CCL_Str_buffer(y) == \"%s\"", CCL_Str_buffer(y));

  CCL_assert(
      strcmp("nil", CCL_Str_buffer(CCL_invoke_method(CCL_nil, "__str__", 0))) == 0,
      "Expected nil.__str__() == 'nil', but nil.__str__() == '%s'",
      CCL_Str_buffer(CCL_invoke_method(CCL_nil, "__str__", 0)));

  CCL_assert(
      strcmp("true", CCL_Str_buffer(CCL_invoke_method(CCL_true, "__str__", 0))) == 0,
      "Expected true.__str__() == 'true', but true.__str__() == '%s'",
      CCL_Str_buffer(CCL_invoke_method(CCL_true, "__str__", 0)));

  CCL_assert(
      strcmp("false", CCL_Str_buffer(CCL_invoke_method(CCL_false, "__str__", 0))) == 0,
      "Expected false.__str__() == 'false', but false.__str__() == '%s'",
      CCL_Str_buffer(CCL_invoke_method(CCL_false, "__str__", 0)));

  fprintf(stderr, "----- All tests successful! -----");
}

int main(int argc, char **argv) {
  test();
  return 0;
}
