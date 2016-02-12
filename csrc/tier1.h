#ifndef CCL_TIER1_H
#define CCL_TIER1_H

#include <memory>
#include <string>

namespace ccl {

using std::shared_ptr;
using std::string;

class Lexer;
class Token;

class Token {
 public:
  const shared_ptr<Lexer> lexer;
  const int i;
  const string type;
  const long int_value;
  const double float_value;
  const string string_value;
  Token(shared_ptr<Lexer> lexer, int i, string type, long value) :
      lexer(lexer), i(i), type(type), int_value(value), float_value(0) {}
  Token(shared_ptr<Lexer> lexer, int i, string type, double value) :
      lexer(lexer), i(i), type(type), int_value(0), float_value(value) {}
  Token(shared_ptr<Lexer> lexer, int i, string type, string value) :
      lexer(lexer), i(i), type(type), int_value(0), float_value(0),
      string_value(value) {}
};


} // ccl

#endif//CCL_TIER1_H
