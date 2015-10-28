/* header */
#include <stddef.h>

typedef struct CCL_Type CCL_Type;
typedef struct CCL_Object CCL_Object;

struct CCL_Type {
  const char *name;
  CCL_Object *(*get_attribute)(CCL_Object*, const char*);
  CCL_Object *(*invoke_method)(CCL_Object*, const char*, int, CCL_Object**);
};

struct CCL_Object {
  CCL_Type *type;
  void *data;
};

void CCL_init();
CCL_Object *CCL_get_attribute(CCL_Object*, const char*);
CCL_Object *CCL_invoke_method(CCL_Object*, const char*, int, ...);
CCL_Object *CCL_Number_new(double);
double CCL_Number_value(CCL_Object*);
CCL_Object *CCL_String_new(const char*);
size_t CCL_String_size(CCL_Object*);
const char* CCL_String_buffer(CCL_Object*);
CCL_Object *CCL_List_new(int, ...);
CCL_Object *CCL_Table_new(int, ...);

/* implementation */
#include <stdarg.h>
#include <stdlib.h>
#include <string.h>

typedef struct CCL_Data_String CCL_Data_String;

struct CCL_Data_Number {
  double value;
};

struct CCL_Data_String {
  size_t size;
  char *buffer;
};

/* test */
void CCL_test() {

}

/* main */
int main(int argc, char **argv) {
  CCL_test();
  return 0;
}