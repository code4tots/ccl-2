// g++ -Wall -Werror -Wpedantic  simple.cc -std=c++11
// header
#include <initializer_list>
#include <iostream>
#include <string>
#include <vector>
#include <unordered_map>

#define TYPE_NUM 1
#define TYPE_STR 2
#define TYPE_LIST 3
#define TYPE_DICT 4
#define TYPE_FUNC 5
#define TYPE_MACRO 6

struct Obj {
  const int uid;
  const int type;
  void *value;
  std::unordered_map<std::string, Obj*> attrs;

  Obj(int t, void *v) : uid(getObjectCount()++), type(t), value(v) {}

  int hash() const { return 0; } // TODO

  operator==(const Obj& other) const {
    if (type != other.type)
      return false;

    switch (type) {
    case TYPE_NUM: return asNum() == other.asNum();
    case TYPE_STR: return asStr() == other.asStr();
    case TYPE_LIST: return asList() == other.asList();
    case TYPE_DICT: return asDict() == other.asDict();
    case TYPE_FUNC:
    case TYPE_MACRO: return uid == other.uid;
    }

    std::cerr << "Invalid type: " << type;
    exit(1);
  }

  double asNum() const { return *static_cast<double*>(value); }
  const std::string& asStr() const { return *static_cast<std::string&>*(value); }

private:
  static int& getObjectCount() {
    static int OBJECT_COUNT = 0;
    return OBJECT_COUNT;
  }
};

inline Obj *X(double v) { return new Obj(TYPE_NUM, new double(v)); }
inline Obj *X(const std::string& v) { return new Obj(TYPE_STR, new std::string(v)); }
inline Obj *X(std::initializer_list<Obj*> v) { return new Obj(TYPE_LIST, new std::vector<Obj*>(v)); }
inline Obj *X(std::initializer_list<pair<Obj*, Obj*> )

// implementation


// main
using namespace std;

int main() {
  X({X(1), X(2), X(3)});
  cout << X(5)->uid << endl;
}
