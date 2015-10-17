// g++ -std=c++11 -Wall -Werror -Wpedantic vm.cc

// Style
// Try to avoid trying to annotate all the const correctness stuff.
// It's fun to do, but C++'s const system isn't perfect, and the
// limitations can really hurt productivity sometimes.
//
// Unfortunately, to play nicely with 'map', for some of it,
// const correctness is mandatory.

// ** header

#include <algorithm>
#include <deque>
#include <functional>
#include <iostream>
#include <map>
#include <string>

struct Pointer;
struct Object;

enum Type {
  NIL_TYPE,   // 0
  NUM_TYPE,   // 1
  STR_TYPE,   // 2
  LIST_TYPE,  // 3
  DICT_TYPE,  // 4
  MACRO_TYPE, // 5
  FUNC_TYPE,  // 6
};

struct Pointer {
  Object * const value;
  Pointer(Object *p): value(p) {}
  Pointer();
  Pointer(double);
  Pointer(const std::string&);
  Pointer(std::initializer_list<Pointer>);
  Object *operator->() { return value; }
  bool operator==(const Pointer&) const;
  bool operator<(const Pointer&) const;
};

struct Object {
  static const std::map<Type, std::string>& getTypeNameTable() {
    static auto table = std::map<Type, std::string>({
        {NIL_TYPE, "NIL_TYPE"},
        {NUM_TYPE, "NUM_TYPE"},
        {STR_TYPE, "STR_TYPE"},
        {LIST_TYPE, "LIST_TYPE"},
        {DICT_TYPE, "DICT_TYPE"},
        {MACRO_TYPE, "MACRO_TYPE"},
        {FUNC_TYPE, "FUNC_TYPE"},
    });
    return table;
  }

  const Type type;
  const double num;
  const std::string str;
private:
  // list and dict are the only object types that can point to other objets.
  // It's important to guard getters and setters for these to make
  // incremental garbage collection easier.
  std::deque<Pointer> list;
  std::map<Pointer, Pointer> dict;
public:
  const std::function<Pointer(Pointer, Pointer)> macro;
  const std::function<Pointer(Pointer)> func;

  Object(): type(NIL_TYPE) 

  // Errors
  std::string NotImplemented(std::string message) const { return "NotImplemented: " + message; }

  // Utility methods

  std::string getTypeName() const {
    const auto& pair = getTypeNameTable().find(type);
    if (pair == getTypeNameTable().end())
      throw "Table name for enum " + std::to_string(type) + " not found";
    return pair->second;
  }

  virtual std::string inspect() const { throw NotImplemented(getTypeName() + ".toString"); }
  virtual std::string toString() const { return inspect(); }

  bool operator==(const Object& x) const {
    return (type == x.type) && eq(x);
  }

  bool operator<(const Object& x) const {
    if (type != x.type)
      return type < x.type;
    switch(type) {
    case 
    }
    return (type != x.type) ? (type < x.type): lt(x);
  }

  virtual bool eq(const Object& x) const=0;
  virtual bool lt(const Object& x) const=0;
};

std::ostream& operator<<(std::ostream& out, const Object& x) { return out << x.toString(); }

// ** implementation

Pointer::Pointer(): Pointer(&Nil::instance) {}
Pointer::Pointer(double n): Pointer(new Object(n)) {}
Pointer::Pointer(const std::string& s): Pointer(new Object(s)) {}
Pointer::Pointer(std::initializer_list<Pointer> l): Pointer(new Object(l)) {}

bool Pointer::operator==(const Pointer& x) const { return *value == *x.value; }
bool Pointer::operator<(const Pointer& x) const { return *value < *x.value; }

// ** main

int main(int argc, char **argv) {
  return 0;
}
