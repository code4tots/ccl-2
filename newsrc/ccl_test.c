/*
Windows:
"C:\Program Files (x86)\Microsoft Visual Studio 14.0\VC\bin\vcvars32.bat"
cl /Za /Feccl_test *.c && ccl_test

OS X:
gcc \
  -std=c89 -pedantic -Wall -Wmissing-braces -Wextra \
  -Wmissing-field-initializers -Wformat=2 -Wswitch-default \
  -Wswitch-enum -Wcast-align -Wpointer-arith -Wbad-function-cast \
  -Wstrict-overflow=5 -Wstrict-prototypes -Winline -Wundef -Wnested-externs \
  -Wcast-qual -Wshadow -Wunreachable-code -Wfloat-equal -Wstrict-aliasing=1 \
  -Wredundant-decls -Wold-style-definition -Werror -ggdb3 -O0 \
  -fno-omit-frame-pointer -fno-common -fstrict-aliasing -Wno-unused-parameter \
  -Wno-format-nonliteral -Wno-float-equal \
  -lm *.c
*/
#include "ccl.h"

#include <stdio.h>
#include <string.h>

void test() {
  CCL_Object *x, *y, *z;

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

  /* basic constants test */
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

  /* basic Dict test */
  x = CCL_new_Dict(0);
  y = CCL_new_Num(10);
  z = CCL_new_Num(20);

  CCL_assert(CCL_Dict_size(x) == 0, "Expected CCL_Dict_size(x) == 0, but found: %d", CCL_Dict_size(x));
  CCL_invoke_method(x, "__setitem__", 2, y, z);
  CCL_assert(CCL_Dict_size(x) == 1, "Expected CCL_Dict_size(x) == 1, but found: %d", CCL_Dict_size(x));
  CCL_assert(
      CCL_Num_value(CCL_invoke_method(x, "__size__", 0)) == 1,
      "Expected x.__size__() == 1 but found: %s",
      CCL_repr(CCL_invoke_method(x, "__size__", 0)));
  CCL_assert(CCL_invoke_method(x, "__getitem__", 1, y) == z, "Expected x[y] == z");
  CCL_assert(CCL_truthy(CCL_invoke_method(x, "__contains__", 1, y)), "Expected y in x");
  CCL_assert(!CCL_truthy(CCL_invoke_method(x, "__contains__", 1, z)), "Expected z not in x");

  /* basic Str test */
  x = CCL_new_Str("hello\nthere");

  CCL_assert(strcmp("hello\nthere", CCL_Str_buffer(CCL_invoke_method(x, "__str__", 0))) == 0, "Expect x == 'hello\nthere'");
  CCL_assert(strcmp("\"hello\\nthere\"", CCL_repr(x)) == 0, "Expect x.__repr__() == \"hello\\nthere\"");

  /* typename */
  CCL_assert(
      strcmp("Str", CCL_Str_buffer(CCL_invoke_method(x, "__typename__", 0))) == 0,
      "Expected x.__typename__() == 'Str' but found %s",
      CCL_Str_buffer(CCL_invoke_method(x, "__typename__", 0)));

  /* misc */
  CCL_assert(
      CCL_Num_value(CCL_invoke_method(
          CCL_new_Num(45), "__cmp__", 1, CCL_new_Str("hi"))) < 0,
      "If the types are incompatible, __cmp__ should compare by name of type ('Num' < 'Str').");

  CCL_assert(
      CCL_Num_value(CCL_invoke_method(
           CCL_new_Str("hi"), "__cmp__", 1,CCL_new_Num(45))) > 0,
      "If the types are incompatible, __cmp__ should compare by name of type ('Str' > 'Num').");

  fprintf(stderr, "----- All tests successful! -----\n");
}

int main(int argc, char **argv) {
  test();
  return 0;
}
