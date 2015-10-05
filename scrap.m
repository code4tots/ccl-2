/* gcc -Wall -Werror -Wpedantic -std=c89 scrap.m -framework Foundation -lobjc && ./a.out */

/* header */
#import <Foundation/Foundation.h>

@interface Macro : NSObject
-(id)initWithBlock:(id(^)(NSMutableDictionary *ctx, NSArray *args)) block;
@property (nonatomic, copy) id(^block)(NSMutableDictionary *ctx, NSArray *args);
@end

@interface Function : NSObject
-(id)initWithBlock:(id(^)(NSArray *args))block;
@property (nonatomic, copy) id(^block)(NSArray *args);
@end

id lookupVariable(NSMutableDictionary *ctx, NSString *key);
id eval(NSMutableDictionary *ctx, id node);

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

id lookupVariable(NSMutableDictionary *ctx, NSString *key) {
  id val = [ctx objectForKey: key];
  id parent;

  if (val != nil)
    return val;

  parent = [ctx objectForKey: @"__parent__"];

  if (parent == nil)
    [NSException raise:@"Variable not found" format:@"with name '%@'.", key];

  return lookupVariable(parent, key);
}

id eval(NSMutableDictionary *ctx, id node) {
  if ([node isKindOfClass: [NSNumber class]])
    return node;

  if ([node isKindOfClass: [NSString class]])
    return lookupVariable(ctx, (NSString *) node);

  if ([node isKindOfClass: [NSArray class]]) {
    NSArray *n = (NSArray *) node;
    NSArray *argexprs;
    NSMutableArray *args;
    id func;
    int i;

    if (n.count == 0)
      [NSException raise: @"Tried to eval an empty list" format:@""];

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

  [NSException raise:@"Invalid eval node type" format:@"node is of %@ type.", [node class]];
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

int main(int argc, char **argv) {
  @autoreleasepool {
    eval(getRootContext(), @10);
    eval(getRootContext(), @[@"quote", @"hi"]);
  }
}
