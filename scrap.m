/* gcc -Wall -Werror -Wpedantic -std=c89 *.m -framework Foundation -lobjc && ./a.out */

/* header */
#import <Foundation/Foundation.h>

#include <ctype.h>
#include <string.h>

@interface Macro : NSObject
-(id)initWithBlock:(id(^)(NSMutableDictionary *ctx, NSArray *args)) block;
@property (nonatomic, copy) id(^block)(NSMutableDictionary *ctx, NSArray *args);
@end

@interface Function : NSObject
-(id)initWithBlock:(id(^)(NSArray *args))block;
@property (nonatomic, copy) id(^block)(NSArray *args);
@end

NSMutableDictionary *find(NSMutableDictionary *ctx, NSString *key);
id lookupVariable(NSMutableDictionary *ctx, NSString *key);
id eval(NSMutableDictionary *ctx, NSDictionary *node);

NSMutableDictionary *getRootContext();

/* implementation */

@implementation Macro
-(id)initWithBlock:(id(^)(NSMutableDictionary *ctx, NSArray *args)) block {
  if (self = [super init])
    self.block = block;
  return self;
}
@end

@implementation Function
-(id)initWithBlock:(id(^)(NSArray *args)) block {
  if (self = [super init])
    self.block = block;
  return self;
}
@end

static Macro *makeMacro(id(^block)(NSMutableDictionary *ctx, NSArray *args)) {
  return [[Macro alloc] initWithBlock: block];
}

static Function *makeFunc(id(^block)(NSArray *args)) {
  return [[Function alloc] initWithBlock: block];
}

NSMutableDictionary *find(NSMutableDictionary *ctx, NSString *key) {
  id parent;

  if ([ctx objectForKey: key] != nil)
    return ctx;

  parent = [ctx objectForKey: @"__parent__"];

  if (parent != nil)
    return find(parent, key);

  return nil;
}

id lookupVariable(NSMutableDictionary *ctx, NSString *key) {
  NSDictionary *c = find(ctx, key);

  if (c == nil)
    [NSException raise:@"Variable not found" format:@"with name '%@'.", key];

  return [c objectForKey: key];
}

void assign(NSMutableDictionary *ctx, NSString *target, id value, BOOL is_declaration) {
  NSMutableDictionary *c = find(ctx, target);

  if (c == nil) {
    if (is_declaration) {
      [ctx setObject: value forKey: target];
    }
    else {
      [NSException raise:@"assigned to declared name" format:@"%@", target];
    }
  }
  else {
    [c setObject: value forKey: target];
  }
}

id eval(NSMutableDictionary *ctx, NSDictionary *node) {
  NSString *type = [node objectForKey: @"type"];

  if ([type isEqualToString: @"assign"]) {
    NSString *target = [node objectForKey: @"target"];
    id val = eval(ctx, [node objectForKey: @"val"]);
    assign(ctx, target, val, false);
    return val;
  }

  if ([type isEqualToString: @"block"]) {
    id last = nil;
    NSArray *vals = [node objectForKey: @"exprs"];
    int i, len = vals.count;

    for (i = 0; i < len; i++)
      last = eval(ctx, [vals objectAtIndex: i]);
    return last;
  }

  if ([type isEqualToString: @"num"] || [type isEqualToString: @"str"])
    return [node objectForKey: @"val"];

  if ([type isEqualToString: @"list"]) {
    NSMutableArray *list = [[NSMutableArray alloc] init];
    NSArray *vals = [node objectForKey: @"vals"];
    int i, len = vals.count;

    for (i = 0; i < len; i++)
      [list addObject: eval(ctx, [vals objectAtIndex: i])];
    return list;
  }

  if ([type isEqualToString: @"name"])
    return lookupVariable(ctx, [node objectForKey: @"val"]);

  if ([type isEqualToString: @"call"]) {
    NSArray *n = [node objectForKey: @"items"], *argexprs;
    NSMutableArray *args;
    id func;
    int i;

    if (n.count == 0)
      [NSException raise: @"Tried to eval an empty call list" format:@""];

    func = eval(ctx, [n objectAtIndex: 0]);
    argexprs = [n subarrayWithRange: NSMakeRange(1, n.count-1)];

    if ([func isKindOfClass: [Macro class]])
      return ((Macro*) func).block(ctx, argexprs);

    args = [[NSMutableArray alloc] init];

    for (i = 1; i < n.count; i++)
      [args addObject: eval(ctx, [argexprs objectAtIndex: i])];

    if ([func isKindOfClass: [Function class]])
      return ((Function *) func).block(args);

    [NSException raise:@"Tried to call an uncallable type" format:@"type is %@", [func class]];
  }

  [NSException raise:@"Invalid eval node type" format:@"node is of %@ type.", type];
  return nil;
}

NSMutableDictionary *getRootContext() {
  return [@{
    @"quote": makeMacro(^id(NSMutableDictionary *ctx, NSArray *args) {
      if (args.count != 1)
        [NSException raise: @"Wrong number of arguments passed to 'quote'" format:@"Expected 1 but found %lu", args.count];
      return [args objectAtIndex: 0];
    }),
    @"len": makeFunc(^id(NSArray *args) {
      id xs;
      if (args.count != 1)
        [NSException raise: @"Wrong number of arguments passed to 'len'" format:@"Expected 1 but found %lu", args.count];
      xs = [args objectAtIndex: 0];

      if ([xs isKindOfClass: [NSArray class]])
        return [NSNumber numberWithInt: ((NSArray *)xs).count];

      if ([xs isKindOfClass: [NSString class]])
        return [NSNumber numberWithInt: ((NSString *)xs).length];

      [NSException raise:@"Can't call len on given object" format:@"object is of type %@.", [xs class]];
      return nil;
    }),
  } mutableCopy];
}

/* main */

NSDictionary *getCclModules();

int main(int argc, char **argv) {
  @autoreleasepool {
    eval(getRootContext(), [getCclModules() objectForKey: @"blarg.ccl"]);
  }
}
