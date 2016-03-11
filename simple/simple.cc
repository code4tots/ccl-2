/* simple.cc
 * A very simple programming language.
 * Uses C++11
 * g++ -Wall -Werror -Wpedantic -std=c++11 simple.cc
 */


/// header
#include <iostream>
#include <vector>
#include <map>
#include <unordered_map>
#include <string>

namespace ccl {

[[ noreturn ]] inline void die(const std::string& message) {
  std::cout << message << std::endl;
  exit(1);
}

class Value {};

class Pointer {
  Value *value;

public:
  explicit Pointer(): value(NULL) {}
  explicit Pointer(Value *p): value(p) {}

  Value& operator*() { return *value; }
  Value* operator->() { return value; }

  // TODO
  bool operator<(const Pointer& other) const { die("not yet implemented"); }
};

class Scope {
  Scope *const parent;
  std::unordered_map<std::string, Pointer> table;
public:
  explicit Scope(Scope *p): parent(p) {}

  Pointer get(const std::string& key) {
    auto it = table.find(key);
    if (it != table.end()) {
      return it->second;
    }
    if (parent != NULL) {
      return parent->get(key);
    }
    die("Name '" + key + "' not found");
  }

  void put(const std::string& key, Pointer value) {
    table[key] = value;
  }
};

class Ast {
public:
  virtual Pointer eval(Scope& scope)=0;
};

class Number: Value {
public:
  const double value;
  explicit Number(double v): value(v) {}
};

class Text: Value {
  const std::string value;
public:
  explicit Text(const std::string& v): value(v) {}
};

class List final: Value {
  std::vector<Pointer> value;
public:
  explicit List(const std::vector<Pointer>& v): value(v) {}
};

class Table final: Value {
  std::map<Pointer, Pointer> value;
public:
  explicit Table(const std::map<Pointer, Pointer>& v): value(v) {}
};

class Callable: Value {
public:
  virtual Pointer call(Scope& scope, std::vector<Ast*> args)=0;
};

class SpecialForm: Callable {
public:
};

class Function: Callable {
public:
  Pointer call(Scope& scope, std::vector<Ast*> args) final {
    std::vector<Pointer> xargs;
    for (auto arg: args) {
      xargs.push_back(arg->eval(scope));
    }
    return callf(xargs);
  }

  virtual Pointer callf(std::vector<Pointer> args)=0;
};

class BuiltinFunction: Function {
public:
  virtual std::string get_name() const=0;
};

class UserFunction final: Function {
  Scope *const scope;
  const std::vector<std::string> names;
  const Ast *body;
public:
  Pointer callf(std::vector<Pointer> args) {
    // TODO
    return Pointer();
  }
};

class Literal final: Ast {
public:
  const Pointer value;
  Literal(const Pointer& p): value(p) {}
  Pointer eval(Scope& scope) override {
    return value;
  }
}

class Name final: Ast {
public:
  const std::string name;
  Name(const std::string& n): name(n) {}
  Pointer eval(Scope& scope) override {
    return scope.get(name);
  }
};

class Form final: Ast {
public:
  const Ast *const f;
  const std::vector<Ast*> args;
  Form(Ast *f, const std::vector<Ast*>& args): f(f), args(args) {}
  Pointer eval(Scope& scope) override {
    return dynamic_cast<Callable*>(&*f->eval(scope))->call(scope, args);
  }
};

}  // namespace ccl

/// implementation
using namespace ccl;


/// main
int main() {}


