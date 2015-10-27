typedef struct CCL_Object CCL_Object;
struct CCL_Object {
  CCL_Object *metadict;
  union {
    double as_num;
    CCL_dict *as_dict;
  } value;
};
