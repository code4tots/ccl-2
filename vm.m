#import <Foundation/Foundation.h>

typedef NSMutableDictionary Dict;

Dict *CCLFind(Dict *ctx, NSString *key, BOOL orelse) {
  Dict *parent;

  if ([ctx objectForKey: key] != nil)
    return ctx;

  parent = [ctx objectForKey: @"__parent__"];

  if (parent != nil)
    return CCLFind(parent, key, orelse);

  if (orelse)
    [NSException raise:@"Variable not found" format: @"with name '%@'.", key];

  return nil;
}

Dict *CCLLookup(Dict *ctx, NSString *key, BOOL orelse) {
  return [CCLFind(ctx, key, orelse) objectForKey: key];
}

void CCLAssign(Dict *ctx, NSString *key, Dict *value) {
  [CCLFind(ctx, key, YES) setObject: value forKey: key];
}

void CCLDeclare(Dict *ctx, NSString *key, Dict *value) {
  [ctx setObject: value forKey: key];
}

Dict *CCLEval(Dict *ctx, NSDictionary *node) {
  NSString *type = [node objectForKey: @"type"];

  if ([type isEqualToString: @"num"] || [type isEqualToString: @"str"])
    return [node mutableCopy];

  if ([type isEqualToString: @"name"])
    return CCLLookup(ctx, [node objectForKey: @"val"], YES);

  if ([type isEqualToString: @"call"]) {
    Dict *f = CCLEval(ctx, [node objectForKey: @"f"]);
    NSArray *args = [node objectForKey: @"args"];

    if ([[f objectForKey: @"type"] isEqualToString: @"macro"])
      return [f objectForKey: @"f"](ctx, args);
  }

  [NSException raise:@"Invalid eval node type" format:@"node is of %@ type.", type];
  return nil;
}

Dict *CCLEvalBlock(Dict *ctx, NSArray *items) {
  Dict *last = [@{@"type": @"num", @"val": @0} mutableCopy];
  int len = [items count], i;

  for (i = 0; i < len; i++)
    last = CCLEval(ctx, [items objectAtIndex: i]);

  return last;
}

Dict *CCLEvalList(Dict *ctx, NSArray *items) {
  NSMutableArray *arr = [[NSMutableArray alloc] init];
  int len = [items count], i;

  for (i = 0; i < len; i++)
    [arr addObject: CCLEval(ctx, [items objectAtIndex: i])];

  return [@{@"type": @"list", @"val": arr} mutableCopy];
}

int main() {
  return 0;
}
