#import <Foundation/Foundation.h>

NSMutableDictionary *find(NSMutableDictionary *ctx, NSString *name) {
  NSMutableDictionary *parent;

  if ([ctx objectForKey: name] != nil)
    return ctx;

  parent = [ctx objectForKey: @"__parent__"];

  if (parent != nil)
    return find(parent, name);

  return nil;
}

id lookup(NSMutableDictionary *ctx, NSString *name) {
  NSMutableDictionary *c = find(ctx, name);
  if (c == nil)
    [NSException raise:@"Variable not found" format: @"with name '%@'.", name];
  return [c objectForKey: name];
}

void assign(NSMutableDictionary *ctx, NSString *name, id val) {
  NSMutableDictionary *c = find(ctx, name);
  if (c == nil)
    [NSException raise:@"Variable not found" format: @"with name '%@'.", name];
  [c setObject: val forKey: name];
}

void declare(NSMutableDictionary *ctx, NSString *name, id val) {
  [ctx setObject: val forKey: name];
}

id eval(NSMutableDictionary *ctx, id node) {
  if ([node isKindOfClass: [NSNumber class]])
    return node;

  if ([node isKindOfClass: [NSString class]])
    return lookup(ctx, (NSString*) node);

  if ([node isKindOfClass: [NSArray class]]) {
    NSArray *n = node;
    id (^f)(NSMutableDictionary *, id) = eval(ctx, [n objectAtIndex: 0]);
    NSMutableArray *args = [[NSMutableArray alloc] init];
    int len = [n count], i;

    for (i = 1; i < len; i++)
      [args addObject: [n objectAtIndex: i]];

    return f(ctx, args);
  }

  [NSException raise:@"Can't eval an object of type" format: @"%@", [node class]];
  return nil;
}

int main(int argc, char **argv) {
  return 0;
}
