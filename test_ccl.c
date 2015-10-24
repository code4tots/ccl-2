/** gcc -Wall -Werror -Wpedantic -std=c89 ccl.c test_ccl.c && ./a.out */
#include "ccl.h"

void CCL_test() {
  /* num tests */
  {
    assert(CCL_num_new(0) == CCL_num_new(0));
    assert(CCL_num_new(0)->value.as_num == 0);
    assert(CCL_num_new(1)->value.as_num == 1);
  }
  /* str tests */
  {
    assert(CCL_str_new("abcdef")->value.as_str.size == 6);
    assert(CCL_strcat(CCL_list_new(3, CCL_str_new("abc"), CCL_str_new("def"), CCL_str_new("123")))->value.as_str.size == 9);
    assert(CCL_cmp(CCL_strcat(CCL_list_new(3, CCL_str_new("abc"), CCL_str_new("def"), CCL_str_new("123"))),
                   CCL_str_new("abcdef123")) == 0);
  }
  /* list tests */
  {
    CCL_Object *list1 = CCL_list_new(2, CCL_num_new(555), CCL_nil),
               *list2 = CCL_list_new(2, CCL_num_new(555), CCL_nil),
               *list3 = CCL_list_new(2, CCL_num_new(556), CCL_nil);
    assert(list1->value.as_list.size == 2);
    assert(CCL_cmp(list1, list2) == 0);
    assert(CCL_cmp(list1, list3) < 0);

    CCL_list_add(list1, CCL_true);
    assert(list1->value.as_list.size == 3);
    assert(list1->value.as_list.buffer[2] == CCL_true);
    assert(CCL_cmp(list1->value.as_list.buffer[2], CCL_true) == 0);
    assert(CCL_cmp(list1, list2) > 0);

    assert(CCL_list_pop(list1) == CCL_true);
    assert(CCL_cmp(list1, list2) == 0);
  }
  /* dict tests */
  {
    CCL_Object *dict = CCL_dict_new(0), *one = CCL_num_new(1), *two = CCL_num_new(2);

    assert(CCL_dict_size(dict) == 0);
    assert(CCL_dict_getitem(dict, CCL_nil) == NULL);

    CCL_dict_setitem(dict, CCL_nil, CCL_nil);
    assert(CCL_dict_size(dict) == 1);
    assert(CCL_dict_getitem(dict, CCL_nil) == CCL_nil);

    CCL_dict_setitem(dict, one, two);
    assert(CCL_dict_size(dict) == 2);
    assert(CCL_dict_getitem(dict, CCL_nil) == CCL_nil);
    assert(CCL_dict_getitem(dict, one) == two);

    CCL_dict_setitem(dict, CCL_nil, one);
    assert(CCL_dict_size(dict) == 2);
    assert(CCL_dict_getitem(dict, CCL_nil) == one);
    assert(CCL_dict_getitem(dict, one) == two);

    CCL_dict_setitem(dict, two, CCL_nil);
    assert(CCL_dict_size(dict) == 3);
    assert(CCL_dict_getitem(dict, CCL_nil) == one);
    assert(CCL_dict_getitem(dict, one) == two);
    assert(CCL_dict_getitem(dict, two) == CCL_nil);
    assert(CCL_dict_getitem(dict, CCL_true) == NULL);
  }
}

/** main */

int main(int argc, char **argv) {
  CCL_init();
  CCL_test();
  return 0;
}
