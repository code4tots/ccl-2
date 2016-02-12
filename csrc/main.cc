// g++ main.cc --std=c++11 -Wall -Werror -Wpedantic
#include "tier1.h"

int main(int argc, char **argv) {
  ccl::Token lexer(nullptr, 0, "ID", "hello");
  return 0;
}
