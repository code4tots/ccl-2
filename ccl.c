/*
gcc -std=c89 -pedantic -Wall -Wmissing-braces -Wextra -Wmissing-field-initializers -Wformat=2 -Wswitch-default -Wswitch-enum -Wcast-align -Wpointer-arith -Wbad-function-cast -Wstrict-overflow=5 -Wstrict-prototypes -Winline -Wundef -Wnested-externs -Wcast-qual -Wshadow -Wunreachable-code -Wfloat-equal -Wstrict-aliasing=1 -Wredundant-decls -Wold-style-definition -Werror -ggdb3 -O0 -fno-omit-frame-pointer -fno-common -fstrict-aliasing -lm ccl.c
*/
/** header */
#include <stddef.h>

typedef struct CCL_Type CCL_Type;
typedef struct CCL_Object CCL_Object;

struct CCL_Type {
  const char *name;
  CCL_Object **(*get_pointer_to_attribute)(CCL_Object*, const char*);
  CCL_Object *(*invoke_method)(CCL_Object*, const char*, int, CCL_Object**);
};

struct CCL_Object {
  CCL_Type *type;
  void *data;
};

void CCL_inititialize(void);
int CCL_has_attribute(CCL_Object*, const char*);
CCL_Object *CCL_get_attribute(CCL_Object*, const char*);
void CCL_set_attribute(CCL_Object*, const char*, CCL_Object*);
CCL_Object *CCL_invoke_method(CCL_Object*, const char*, int, ...);

/** nil header */
extern CCL_Object *CCL_nil;
void CCL_inititialize_Nil(void);

/** bool header */
extern CCL_Object *CCL_true;
extern CCL_Object *CCL_false;
void CCL_inititialize_Bool(void);

/** number header */
void CCL_inititialize_Number(void);
CCL_Object *CCL_new_Number(double);

/** string header */
void CCL_inititialize_String(void);
CCL_Object *CCL_new_String(const char*);

/** list header */
void CCL_inititialize_List(void);
CCL_Object *CCL_new_List(int, ...);

/** table header */
void CCL_inititialize_Table(void);
CCL_Object *CCL_new_Table(int, ...);

/** test */
#include <assert.h>
void CCL_test() {
}

/** main */
int main() {
  CCL_inititialize();
  CCL_test();
  return 0;
}

/** implementation */
#include <stdarg.h>
#include <stdlib.h>

static int CCL_inititialized = 0;

void CCL_inititialize(void) {
  if (CCL_inititialized)
    return;

  CCL_inititialized = 1;
  CCL_inititialize_Nil();
  CCL_inititialize_Bool();
  CCL_inititialize_Number();
  CCL_inititialize_String();
  CCL_inititialize_List();
  CCL_inititialize_Table();
}

int CCL_has_attribute(CCL_Object *me, const char *name) {
  return me->type->get_pointer_to_attribute(me, name) != NULL;
}

CCL_Object *CCL_get_attribute(CCL_Object *me, const char *name) {
  CCL_Object **pointer = me->type->get_pointer_to_attribute(me, name);

  if (pointer == NULL) {
  	fprintf(stderr, "get_attribute: Object of type '%s' does not have attribute named '%s'\n", me->type->name, name);
  	exit(1);
  }

  return *pointer;
}

void CCL_set_attribute(CCL_Object *me, const char *name, CCL_Object *value) {
  CCL_Object **pointer = me->type->get_pointer_to_attribute(me, name);

  if (pointer == NULL) {
  	fprintf(stderr, "set_attribute: Object of type '%s' does not have attribute named '%s'\n", me->type->name, name);
  	exit(1);
  }

  *pointer = value;
}

CCL_Object *CCL_invoke_method(CCL_Object *me, const char *name, int argc, ...) {
  CCL_Object *result, **argv = malloc(sizeof(CCL_Object*) * argc);
  va_list ap;
  int i;

  va_start(ap, argc);

  for (i = 0; i < argc; i++)
    argv[i] = va_arg(ap, CCL_Object*);

  result = me->type->invoke_method(me, name, argc, argv);

  free(argv);

  va_end(ap);

  if (result == NULL) {
    fprintf(stderr, "invoke_method: Object of type '%s' does not have method named '%s'\n", me->type->name, name);
    eixt(1);
  }

  return result;
}

/** nil implementation */
CCL_Object *CCL_nil;
void CCL_inititialize_Nil(void) {
}

/** bool implementation */
CCL_Object *CCL_true;
CCL_Object *CCL_false;
void CCL_inititialize_Bool(void) {
}

/** number implementation */
typedef struct CCL_Data_Number CCL_Data_Number;

struct CCL_Data_Number {
  double value;
};

void CCL_inititialize_Number(void) {
}

CCL_Object *CCL_new_Number(double value);

/** string implementation */
typedef struct CCL_Data_String CCL_Data_String;

struct CCL_Data_String {
  char *buffer;
  size_t size;
};

void CCL_inititialize_String(void) {
}

CCL_Object *CCL_new_String(const char*);

/** list implementation */
typedef struct CCL_Data_List CCL_Data_List;

struct CCL_Data_List {
  CCL_Object **buffer;
  size_t size, capacity;
};

void CCL_inititialize_List(void) {
}

CCL_Object *CCL_new_List(int, ...);

/** table implementation */
typedef struct CCL_Data_Table CCL_Data_Table;

struct CCL_Data_Table {
  CCL_Data_Table *parent, *children[2];
  size_t size;
};

void CCL_inititialize_Table(void) {
}

CCL_Object *CCL_new_Table(int, ...);
