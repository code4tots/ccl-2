/*
Windows:
cl /Za ccl_test.c ccl_base.c ccl_nil.c ccl_bool.c && ccl_test
*/
#include "ccl.h"

#include <stdio.h>

void test() {
  printf("Hello world!\n");
  CCL_invoke_method(CCL_nil, "__str__", 0);
}

int main(int argc, char **argv) {
  test();
  return 0;
}
