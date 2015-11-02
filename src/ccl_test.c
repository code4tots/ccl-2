/*
Windows: cl /Za ccl_test.c ccl_base.c ccl_nil.c ccl_bool.c && ccl_test
OS X: gcc -std=c89 -pedantic -Wall -Wmissing-braces -Wextra -Wmissing-field-initializers -Wformat=2 -Wswitch-default -Wswitch-enum -Wcast-align -Wpointer-arith -Wbad-function-cast -Wstrict-overflow=5 -Wstrict-prototypes -Winline -Wundef -Wnested-externs -Wcast-qual -Wshadow -Wunreachable-code -Wfloat-equal -Wstrict-aliasing=1 -Wredundant-decls -Wold-style-definition -Werror -ggdb3 -O0 -fno-omit-frame-pointer -fno-common -fstrict-aliasing -Wno-unused-parameter -Wno-format-nonliteral -lm *.c
*/
#include "ccl.h"

#include <stdio.h>

void test() {
  CCL_invoke_method(CCL_nil, "__str__", 0);
  CCL_invoke_method(CCL_new_Num(5), "__str__", 0);
}

int main(int argc, char **argv) {
  test();
  return 0;
}
