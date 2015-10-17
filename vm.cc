// g++ -std=c++11 -Wall -Werror -Wpedantic vm.cc

// TODO: Garbage collection.
// TODO: Better error handling (e.g. better than 'throw type').

//** header

#include <algorithm>
#include <deque>
#include <functional>
#include <iostream>
#include <map>
#include <string>

enum Type {
  NIL_TYPE,   // 0
  NUM_TYPE,   // 1
  STR_TYPE,   // 2
  LIST_TYPE,  // 3
  DICT_TYPE,  // 4
  MACRO_TYPE, // 5
  FUNC_TYPE,  // 6
};

struct Pointer;
struct Object;

extern Pointer ROOT;

struct Pointer {
  Object *value;
  Pointer();
  Pointer(Object *p) : value(p) {}
  Pointer(double n);
  Pointer(const std::string& s);
  Pointer(const char *s): Pointer(std::string(s)) {}
  Pointer(std::initializer_list<Pointer> l);
  Object *operator->() { return value; }
  const Object *operator->() const { return value; }

  bool operator==(const Pointer& p) const;
  bool operator<(const Pointer& p) const;
};

struct Object {
  const Type type;
  const double num;
  const std::string str;

  // list and dict are kept private so that I can have
  // strict write barriers to make writing an incremental
  // garbage collector simple.
  // Other members can be kept public since they can't contain
  // references to other objects. And also they are all const.
private:
  std::deque<Pointer> list;
  std::map<Pointer, Pointer> dict;

public:
  const std::function<Pointer(Pointer, Pointer)> macro;
  const std::function<Pointer(Pointer)> func;

  Object(double n): type(NUM_TYPE), num(n) {}
  Object(const std::string& s): type(STR_TYPE), num(0), str(s) {}
  Object(std::initializer_list<Pointer> l): type(LIST_TYPE), num(0), list(l) {}
  Object(std::initializer_list<std::pair<const Pointer, Pointer> > d): type(DICT_TYPE), num(0), dict(d) {}
  Object(const std::string& s, const std::function<Pointer(Pointer, Pointer)>& m): type(MACRO_TYPE), num(0), str(s), macro(m) {}
  Object(const std::function<Pointer(Pointer, Pointer)>& m): type(MACRO_TYPE), num(0), macro(m) {}
  Object(const std::string& s, const std::function<Pointer(Pointer)>& f): type(FUNC_TYPE), num(0), str(s), func(f) {}
  Object(const std::function<Pointer(Pointer)>& f): type(FUNC_TYPE), num(0), func(f) {}

  // There should be only one copy of nil.
private:
  Object(): type(NIL_TYPE), num(0) {}

public:

  static Object Nil;

  bool operator==(const Object& x) const;
  bool operator<(const Object& x) const;

  int len() const {
    switch(type) {
    case STR_TYPE: return str.size();
    case LIST_TYPE: return list.size();
    case DICT_TYPE: return dict.size();
    default: throw type;
    }
  }

  bool contains(const Pointer& item) const {
    switch(type) {
    case LIST_TYPE:
      return std::find(list.begin(), list.end(), item) != list.end();
    case DICT_TYPE:
      return dict.find(item) != dict.end();
    default: throw type;
    }
  }

  void set(const Pointer& key, const Pointer& value) {
    switch(type) {
    case LIST_TYPE:
      if (key->type != NUM_TYPE)
        throw key->type;
      if (key->num < 0 || key->num > list.size())
        throw key->num;
      list[key->num] = value;
      break;
    case DICT_TYPE:
      dict[key] = value;
      break;
    default: throw type;
    }
  }

  Pointer get(const Pointer& key) const {
    switch(type) {
    case STR_TYPE: {
      if (key->type != NUM_TYPE)
        throw key->type;
      if (key->num < 0 || key->num > str.size())
        throw key->num;
      return str[key->num];
    }
    case LIST_TYPE: {
      if (key->type != NUM_TYPE)
        throw key->type;
      if (key->num < 0 || key->num > list.size())
        throw key->num;
      return list[key->num];
    }
    case DICT_TYPE: {
      auto pair = dict.find(key);
      if (pair == dict.end())
        throw key;
      return pair->second;
    }
    default: throw type;
    }
  }

  std::string cc_repr() const {
    switch(type) {
    case NIL_TYPE: return "nil";
    case NUM_TYPE: return std::to_string(num);
    case STR_TYPE: {
      std::string s("\"");
      for (char c: str) {
        switch(c) {
        case '\n': s += "\\n";
        case '\t': s += "\\t";
        case '\\': s += "\\\\";
        case '\"': s += "\\\"";
        default: s += c;
        }
      }
      s += "\"";
      return s;
    }
    case LIST_TYPE: {
      std::string s("[");
      for (int i = 0; i < list.size(); i++) {
        if (i > 0)
          s += ", ";
        s += list[i]->cc_repr();
      }
      s += "]";
      return s;
    }
    case DICT_TYPE: {
      std::string s("{");
      bool first = true;
      for (const auto& pair: dict) {
        if (!first)
          s += ", ";
        s += pair.first->cc_repr();
        s += ": ";
        s += pair.second->cc_repr();
        first = false;
      }
      s += "}";
      return s;
    }
    case MACRO_TYPE:
      if (!str.empty())
        return "<macro " + str + ">";
      else
        return "<macro " + std::to_string((long long) this) + ">";
    default: throw type;
    }
  }

  std::string cc_str() const {
    switch(type) {
    case STR_TYPE: return str;
    default: return cc_repr();
    }
  }

  Pointer slice(int start) const {
    return slice(start, len());
  }

  Pointer slice(int start, int end) const {
    if (end > len())
      throw end;
    switch(type) {
    case STR_TYPE:
      return std::string(str.begin()+start, str.begin()+end);
    default: throw type;
    }
  }

  Pointer lookup(const Pointer key) const {
    if (type != DICT_TYPE)
      throw type;

    if (contains(key))
      return get(key);

    if (contains("__parent__"))
      return get("__parent__")->lookup(key);

    throw key;
  }

  void declare(const Pointer key, Pointer value) {
    if (type != DICT_TYPE)
      throw type;

    set(key, value);
  }

  void assign(const Pointer key, Pointer value) {
    if (type != DICT_TYPE)
      throw type;

    if (contains(key)) {
      set(key, value);
      return;
    }

    if (contains("__parent__")) {
      get("__parent__")->assign(key, value);
      return;
    }

    throw key;
  }

  Pointer eval(Pointer context) const {
    switch(type) {
    case NIL_TYPE: return Pointer();
    case NUM_TYPE: return Pointer(num);
    case STR_TYPE: return context->lookup(str);
    case LIST_TYPE: {
      Pointer f = get(0)->eval(context);
      return f->call(slice(1));
    }
    default: throw type;
    }
  }
};

std::ostream& operator<<(std::ostream& out, const Object& x);
std::ostream& operator<<(std::ostream& out, const Pointer& x);

Pointer makeDict(std::initializer_list<std::pair<const Pointer, Pointer> > d);

//** implementation

Object Object::Nil;

Pointer ROOT = []() {
  return Pointer();
}();

Pointer::Pointer(): Pointer(&Object::Nil) {}
Pointer::Pointer(double n): Pointer(new Object(n)) {}
Pointer::Pointer(const std::string& s): Pointer(new Object(s)) {}
Pointer::Pointer(std::initializer_list<Pointer> l): Pointer(new Object(l)) {}

bool Pointer::operator==(const Pointer& p) const {
  return *value == *p.value;
}

bool Pointer::operator<(const Pointer& p) const {
  return *value < *p.value;
}

bool Object::operator==(const Object& x) const {
  if (type != x.type)
    return false;

  switch(type) {
  case NUM_TYPE: return num == x.num;
  case STR_TYPE: return str == x.str;
  case LIST_TYPE: return list == x.list;
  case DICT_TYPE: return dict == x.dict;
  case MACRO_TYPE: return this == &x;
  default: throw type;
  }
}

bool Object::operator<(const Object& x) const {
  if (type != x.type)
    return type < x.type;

  switch(type) {
  case NUM_TYPE: return num < x.num;
  case STR_TYPE: return str < x.str;
  case LIST_TYPE: return list < x.list;
  case DICT_TYPE: return dict < x.dict;
  case MACRO_TYPE: return this < &x;
  default: throw type;
  }
}

std::ostream& operator<<(std::ostream& out, const Object& x) {
  return out << x.cc_str();
}

std::ostream& operator<<(std::ostream& out, const Pointer& x) {
  return out << *x.value;
}

Pointer makeDict(std::initializer_list<std::pair<const Pointer, Pointer> > d) {
  return Pointer(new Object(d));
}

//** main

#include <iostream>
using namespace std;

int main(int argc, char **argv) {
  Pointer p = 8, q = {1, 2, {3, "hi"}}, r = makeDict({{1, 2}});
  cout << p << ' ' << q << endl;
  cout << r << endl;
  cout << p->num << endl;
  cout << &r << endl;
  cout << Pointer({1, 2, 3})->contains(3) << endl;
  cout << makeDict({{1, 2}, {3, 4}})->contains(3) << endl;
  cout << Pointer() << endl;
  r->set(5, 1235);
  cout << r->get(5) << endl;
  return 0;
}
