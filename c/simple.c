/* gcc -std=c89 -Wall -Werror -Wpedantic simple.c && cp simple.c{,c} && g++ -std=c++98 -Wall -Werror -Wpedantic simple.cc */

/* header */
#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define CCL_TYPE_NUM 1
#define CCL_TYPE_STR 2
#define CCL_TYPE_LIST 3
#define CCL_TYPE_DICT 4
#define CCL_TYPE_MACRO 5
#define CCL_TYPE_FUNCTION 6
#define CCL_TYPE_LAMBDA 7
#define CCL_TYPE_METHOD 8

typedef struct CCL_Object CCL_Object;
typedef struct CCL_Str CCL_Str;
typedef struct CCL_List CCL_List;
typedef struct CCL_Dict CCL_Dict;
typedef struct CCL_Lambda CCL_Lambda;
typedef struct CCL_Method CCL_Method;

struct CCL_Str {
  int len;
  char *buf;
};

/* TODO: Smarter List realloc strategy */
struct CCL_List {
  int len;
  CCL_Object **buf;
};

/* TODO: Make dict be a hash table instead of a naive associative array */
struct CCL_Dict {
  int len;
  CCL_Object **buf;
};

struct CCL_Lambda {
  CCL_Object *ctx, *node;
};

struct CCL_Method {
  CCL_Object *owner, *f;
};

struct CCL_Object {
  int type;
  union {
    double as_num;
    CCL_Str as_str;
    CCL_List as_list;
    CCL_Dict as_dict;
    CCL_Object *(*as_macro)(CCL_Object *ctx, CCL_Object *args);
    CCL_Object *(*as_function)(CCL_Object *args);
    CCL_Lambda as_lambda;
    CCL_Method as_method;
  } val;
};

void CCL_err(const char *message);

CCL_Object *CCL_makeNum(double d);
CCL_Object *CCL_makeStr(const char *s);
CCL_Object *CCL_makeList();
CCL_Object *CCL_makeDict();

int CCL_size(CCL_Object *obj);

int CCL_cmp(const CCL_Object * lhs, const CCL_Object * rhs);

CCL_Object *CCL_str(CCL_Object *obj);

CCL_Object *CCL_Str_cat(CCL_Object *lhs, CCL_Object *rhs);

void CCL_List_push(CCL_Object *list, CCL_Object *item);
CCL_Object *CCL_List_pop(CCL_Object *list);
CCL_Object *CCL_List_get(CCL_Object *list, int i);
void CCL_List_set(CCL_Object *list, int i, CCL_Object *item);

CCL_Object *CCL_Dict_get(CCL_Object *dict, CCL_Object *key, CCL_Object *def);
void CCL_Dict_set(CCL_Object *dict, CCL_Object *key, CCL_Object *value);
CCL_Object *CCL_Dict_keys(CCL_Object *dict);

CCL_Object *CCL_lex(const CCL_Object *string);
CCL_Object *CCL_parse(const CCL_Object *tokens);

/* implementation */

static int CCL_Dict_qsort_cmp(const void *lhs, const void *rhs) {
  return CCL_cmp(*(CCL_Object**)lhs, *(CCL_Object**)rhs);
}

void CCL_err(const char *message) {
  fprintf(stderr, "%s\n", message);
  exit(1);
}

CCL_Object *CCL_make(int type) {
  CCL_Object *ret = (CCL_Object*) malloc(sizeof(CCL_Object));
  ret->type = type;
  return ret;
}

CCL_Object *CCL_makeNum(double d) {
  CCL_Object *ret = CCL_make(CCL_TYPE_NUM);

  ret->val.as_num = d;
  return ret;
}

CCL_Object *CCL_makeStr(const char *s) {
  const int len = strlen(s);
  CCL_Object *ret = CCL_make(CCL_TYPE_STR);

  ret->val.as_str.len = len;
  ret->val.as_str.buf = (char*) malloc(sizeof(char) * (len+1));
  strcpy(ret->val.as_str.buf, s);
  return ret;
}

CCL_Object *CCL_makeList() {
  CCL_Object *ret = CCL_make(CCL_TYPE_LIST);

  ret->val.as_list.len = 0;
  ret->val.as_list.buf = NULL;
  return ret;
}

CCL_Object *CCL_makeDict() {
  CCL_Object *ret = CCL_make(CCL_TYPE_DICT);

  ret->val.as_dict.len = 0;
  ret->val.as_dict.buf = NULL;
  return ret;
}

int CCL_size(CCL_Object *obj) {
  switch (obj->type) {
  case CCL_TYPE_LIST: return obj->val.as_list.len;
  case CCL_TYPE_DICT: return obj->val.as_dict.len;
  default: CCL_err("CCL_size err"); return 0;
  }
}

int CCL_cmp(const CCL_Object * lhs, const CCL_Object * rhs) {
  if (lhs->type != rhs->type)
    return lhs->type - rhs->type;

  switch(lhs->type) {
  case CCL_TYPE_NUM:
    return lhs->val.as_num <  rhs->val.as_num ? -1 :
           lhs->val.as_num == rhs->val.as_num ? 0  : 1;
  case CCL_TYPE_STR:
    return strcmp(lhs->val.as_str.buf, rhs->val.as_str.buf);
  case CCL_TYPE_LIST: {
      const int len = lhs->val.as_list.len < rhs->val.as_list.len ? lhs->val.as_list.len : rhs->val.as_list.len;
      int i;

      for (i = 0; i < len; i++) {
        const int cmp = CCL_cmp(lhs->val.as_list.buf[i], rhs->val.as_list.buf[i]);
        if (cmp != 0)
          return cmp;
      }

      return lhs->val.as_list.len - rhs->val.as_list.len;
    }
  case CCL_TYPE_DICT:{
      const int len = lhs->val.as_dict.len < rhs->val.as_dict.len ? lhs->val.as_dict.len : rhs->val.as_dict.len;
      int i;

      for (i = 0; i < 2*len; i++) {
        const int cmp = CCL_cmp(lhs->val.as_dict.buf[i], rhs->val.as_dict.buf[i]);
        if (cmp != 0)
          return cmp;
      }

      return lhs->val.as_dict.len - rhs->val.as_dict.len;
    }
  case CCL_TYPE_MACRO:
  case CCL_TYPE_FUNCTION:
  case CCL_TYPE_LAMBDA:
    return lhs - rhs;
  default:
    CCL_err("CCL_cmp (type err)"); return 0;
  }
}

CCL_Object *CCL_str(CCL_Object *obj) {
  switch (obj->type) {
  case CCL_TYPE_NUM: {
    char buf[50];
    sprintf(buf, "%G", obj->val.as_num); /* TODO: Find out max number of digits possible here */
    return CCL_makeStr(buf);
  }
  case CCL_TYPE_STR: return obj;
  case CCL_TYPE_LIST: {
    CCL_Object *s = CCL_makeStr("[");
    int i;

    for (i = 0; i < obj->val.as_list.len; i++) {
      if (i != 0)
        s = CCL_Str_cat(s, CCL_makeStr(" "));
      s = CCL_Str_cat(s, CCL_str(obj->val.as_list.buf[i]));
    }
    s = CCL_Str_cat(s, CCL_makeStr("]"));
    return s;
  }
  case CCL_TYPE_DICT: return CCL_makeStr("Dict"); /* TODO */
  case CCL_TYPE_MACRO: return CCL_makeStr("Macro");
  case CCL_TYPE_FUNCTION: return CCL_makeStr("Function");
  case CCL_TYPE_LAMBDA: return CCL_makeStr("Lambda");
  default:
    fprintf(stderr, "type = %d ", obj->type);
    CCL_err("CCL_str (type err)"); return NULL;
  }
}

CCL_Object *CCL_Str_cat(CCL_Object *lhs, CCL_Object *rhs) {
  char *s;
  CCL_Object *ret;

  if (lhs->type != CCL_TYPE_STR)
    CCL_err("CCL_Str_cat lhs (type err)");
  if (rhs->type != CCL_TYPE_STR)
    CCL_err("CCL_Str_cat rhs (type err)");

  s = (char*) malloc(sizeof(char) * (lhs->val.as_str.len + rhs->val.as_str.len + 1));
  strcpy(s, lhs->val.as_str.buf);
  strcpy(s + lhs->val.as_str.len, rhs->val.as_str.buf);
  ret = CCL_makeStr(s);
  free(s);

  return ret;
}

void CCL_List_push(CCL_Object *list, CCL_Object *item) {
  list->val.as_list.len++;
  list->val.as_list.buf = (CCL_Object**) realloc(list->val.as_list.buf, list->val.as_list.len);
  list->val.as_list.buf[list->val.as_list.len-1] = item;
}

CCL_Object *CCL_List_pop(CCL_Object *list) {
  if (list->type != CCL_TYPE_LIST)
    CCL_err("CCL_List_pop (type err)");
  if (list->val.as_list.len == 0)
    CCL_err("CCL_List_pop (empty list err)");
  return list->val.as_list.buf[--list->val.as_list.len];
}

CCL_Object *CCL_List_get(CCL_Object *list, int i) {
  if (list->type != CCL_TYPE_LIST)
    CCL_err("CCL_List_get (type err)");
  if (i < 0 || i >= list->val.as_list.len)
    CCL_err("CCL_List_get (index err)");
  return list->val.as_list.buf[i];
}

void CCL_List_set(CCL_Object *list, int i, CCL_Object *item) {
  if (list->type != CCL_TYPE_LIST)
    CCL_err("CCL_List_set (type err)");
  if (i < 0 || i >= list->val.as_list.len)
    CCL_err("CCL_List_set (index err)");
  list->val.as_list.buf[i] = item;
}

CCL_Object *CCL_Dict_get(CCL_Object *dict, CCL_Object *key, CCL_Object *def) {
  int i;
  if (dict->type != CCL_TYPE_DICT)
    CCL_err("CCL_Dict_get (type err)");
  for (i = 0; i < dict->val.as_dict.len; i++)
    if (CCL_cmp(key, dict->val.as_dict.buf[2*i]) == 0)
      return dict->val.as_dict.buf[2*i+1];
  return def;
}

void CCL_Dict_set(CCL_Object *dict, CCL_Object *key, CCL_Object *value) {
  int i = 0, j;
  if (dict->type != CCL_TYPE_DICT)
    CCL_err("CCL_Dict_set (type err)");
  while (i < dict->val.as_dict.len && CCL_cmp(dict->val.as_dict.buf[2*i], key) < 0)
    i++;
  if (i < dict->val.as_dict.len && CCL_cmp(dict->val.as_dict.buf[2*i], key) == 0) {
    dict->val.as_dict.buf[2*i+1] = value;
    return;
  }
  dict->val.as_dict.len++;
  dict->val.as_dict.buf = (CCL_Object**) realloc(dict->val.as_dict.buf, 2*dict->val.as_dict.len);
  for (j = i+1; j < dict->val.as_dict.len; j++) {
    dict->val.as_dict.buf[2*j] = dict->val.as_dict.buf[2*j-2];
    dict->val.as_dict.buf[2*j+1] = dict->val.as_dict.buf[2*j-1];
  }
  dict->val.as_dict.buf[2*i] = key;
  dict->val.as_dict.buf[2*i+1] = value;
  /* This qsort here is necessary so that CCL_cmp behaves properly */
  qsort(dict->val.as_dict.buf, dict->val.as_dict.len, 2*sizeof(CCL_Object*), &CCL_Dict_qsort_cmp);
}

CCL_Object *CCL_Dict_keys(CCL_Object *dict) {
  CCL_Object *ret;
  int i;
  if (dict->type != CCL_TYPE_DICT)
    CCL_err("CCL_Dict_keys (type err)");
  ret = CCL_makeList();
  for (i = 0; i < dict->val.as_dict.len; i++)
    CCL_List_push(ret, dict->val.as_dict.buf[2*i]);
  return ret;
}

CCL_Object *CCL_lex(const CCL_Object *string) {
  int len, i = 0, j, seenDot;
  const char * s;
  CCL_Object *toks;

  if (string->type != CCL_TYPE_STR)
    CCL_err("CCL_lex string (type err)");

  len = string->val.as_str.len;
  s = string->val.as_str.buf;

  toks = CCL_makeList();

  while (1) {

    while (i < len && isspace(s[i]))
      i++;

    if (i >= len)
      break;

    j = i;

    if (s[i] == '(') {
      CCL_Object *tok = CCL_makeList();
      CCL_List_push(tok, CCL_makeStr("("));
      CCL_List_push(toks, tok);
      i++;
      continue;
    }

    if (s[i] == ')') {
      CCL_Object *tok = CCL_makeList();
      CCL_List_push(tok, CCL_makeStr(")"));
      CCL_List_push(toks, tok);
      i++;
      continue;
    }

    /* num */
    seenDot = 0;
    if (s[i] == '-')
      i++;
    if (i < len && s[i] == '.')
      seenDot = 1, i++;
    if (i < len && isdigit(s[i])) {
      CCL_Object *tok = CCL_makeList();

      while (i < len && isdigit(s[i]))
        i++;
      if (!seenDot && s[i] == '.')
        i++;
      while (i < len && isdigit(s[i]))
        i++;

      CCL_List_push(tok, CCL_makeStr("num"));
      CCL_List_push(tok, CCL_makeNum(strtod(s+j, NULL)));
      CCL_List_push(toks, tok);
      continue;
    }
    else i = j;

    CCL_err("CCL_lex (unrecognized token)");
  }

  return toks;
}

/* main */

int main(int argc , char **argv) {
  CCL_Object *d = CCL_makeDict();
  CCL_Object *keys = CCL_Dict_keys(d);
  CCL_Object *list = CCL_makeList();
  CCL_Object *toks = NULL;

  printf("%d\n", CCL_size(d));
  printf("%d\n", CCL_size(keys));

  CCL_Dict_set(d, CCL_makeStr("hi"), CCL_makeStr("there"));
  CCL_Dict_set(d, CCL_makeStr("hi"), CCL_makeStr("there"));
  CCL_Dict_set(d, CCL_makeStr("hoi"), CCL_makeStr("there"));

  printf("%d\n", CCL_size(d));
  printf("%d\n", CCL_size(keys));

  keys = CCL_Dict_keys(d);

  printf("%d\n", CCL_size(d));
  printf("%d\n", CCL_size(keys));

  CCL_List_push(list, keys);

  printf("list len = %d\n", CCL_size(list));

  toks = CCL_lex(CCL_makeStr("( 5 6 7)"));

  printf("number of tokens = %d\n", CCL_size(toks));
  printf("toks = %s\n", CCL_str(toks)->val.as_str.buf);
}
