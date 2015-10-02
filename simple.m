/* gcc -Wall -std=c99 simple.m -framework Foundation -lobjc && ./a.out */

// header
#import <Foundation/Foundation.h>

@class Dict;

@interface Obj : NSObject <NSCopying, NSMutableCopying>
-(id)init;
-(Obj *)evalWithContext:(Dict *)ctx;
-(id)copyWithZone:(NSZone *)zone;
-(id)mutableCopyWithZone:(NSZone *)zone;
@end

@interface Num : Obj
@property (retain) NSNumber *value;
-(id)initWithValue:(NSNumber *)val;
-(Obj *)evalWithContext:(Dict *)ctx;
-(id)copyWithZone:(NSZone *)zone;
-(BOOL)isEqual:(id)object;
-(NSUInteger) hash;
@end

@interface Str : Obj
@property (retain) NSString *value;
-(id)initWithValue:(NSString *)val;
-(Obj *)evalWithContext:(Dict *)ctx;
-(id)copyWithZone:(NSZone *)zone;
-(BOOL)isEqual:(id)object;
-(NSUInteger) hash;
@end

@interface List : Obj
@property (retain) NSMutableArray *value;
-(id)initWithValue:(NSArray *)val;
-(Obj *)evalWithContext:(Dict *)ctx;
-(id)copyWithZone:(NSZone *)zone;
-(BOOL)isEqual:(id)object;
-(NSUInteger) hash;
@end

@interface Dict : Obj
@property (retain) NSMutableDictionary *value;
-(id)initWithValue:(NSDictionary *)val;
-(id)copyWithZone:(NSZone *)zone;
-(BOOL)isEqual:(id)object;
-(NSUInteger) hash;
// Context methods
-(Obj*)lookupVariable:(Str *)name;
-(Obj*)xlookupVariable:(Str *)name;
-(void)defineVariable:(Str *)name withValue:(Obj *)val;
-(void)assignVariable:(Str *)name withValue:(Obj *)val;
@end

@interface Macro : Obj
@property (nonatomic, copy) Obj *(^value)(Dict *, NSArray *);
-(id)initWithValue: (Obj *(^)(Dict *, NSArray *))val;
@end

@interface Function : Obj
@property (nonatomic, copy) Obj *(^value)(NSArray *);
-(id)initWithValue: (Obj *(^)(NSArray *))val;
@end

Dict *getRootContext();

List *parse(NSString *s);
List *xparse(NSString *s);

// implementation

static int objcnt = 0;

static id err(NSString *message) {
  // TODO: Better error handling.
  NSLog(@"%@", message);
  exit(1);
}

@implementation Obj {
  int uid;
}

+ (void)initialize {
  if (self == [Obj class]) {
  }
}

- (id)init {
  self = [super init];
  if (self) {
    uid = objcnt++;
  }
  return self;
}

-(Obj *)evalWithContext:(Dict *) ctx {
  return err(@"eval called on an object that doesn't support it");
}

-(id)copyWithZone:(NSZone *)zone {
  return self; // TODO
}

-(id)mutableCopyWithZone:(NSZone *)zone {
  // All subclasses of Obj should be mutable anyway.
  return [self copyWithZone: zone];
}
@end

@implementation Num
-(id)initWithValue:(NSNumber *)val {
  self = [super init];
  if (self) {
    self.value = val;
  }
  return self;
}
-(Obj *)evalWithContext:(Dict *) ctx {
  return self;
}
-(id)copyWithZone:(NSZone *)zone {
  return [[Num alloc] initWithValue: self.value];
}
-(BOOL)isEqual:(id)object {
  if (self == object)
    return YES;
  if (![object isKindOfClass: [Num class]])
    return NO;
  return [self.value isEqual:((Num*) object).value];
}
-(NSUInteger) hash {
  return [self.value hash];
}
-(NSString *)description {
  return [self.value description];
}
@end

@implementation Str
-(id)initWithValue:(NSString *)val {
  self = [super init];
  if (self) {
    self.value = val;
  }
  return self;
}
-(Obj *)evalWithContext:(Dict *)ctx {
  return [ctx lookupVariable: self];
}
-(id)copyWithZone:(NSZone *)zone {
  return [[Str alloc] initWithValue: self.value];
}
-(BOOL)isEqual:(id)object {
  if (self == object)
    return YES;
  if (![object isKindOfClass: [Str class]])
    return NO;
  return [self.value isEqual:((Str*) object).value];
}
-(NSUInteger) hash {
  return [self.value hash];
}
-(NSString *)description {
  return [self.value description];
}
@end

@implementation List
-(id)initWithValue:(NSArray *)val {
  self = [super init];
  if (self) {
    self.value = [val mutableCopy];
  }
  return self;
}
-(Obj *)evalWithContext:(Dict *) ctx {
  Obj *f = [[self.value objectAtIndex: 0] evalWithContext: ctx];
  NSArray *argexprs = [self.value subarrayWithRange: NSMakeRange(1, [self.value count]-1)];
  if ([f class] == [Macro class]) {
    return ((Macro *) f).value(ctx, argexprs);
  }
  if ([f class] == [Function class]) {
    NSMutableArray *args = [[NSMutableArray alloc] init];
    [argexprs enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
      [args addObject: [((Obj*)obj) evalWithContext:ctx]];
    }];
    return ((Function *) f).value(args);
  }
  return err([NSString stringWithFormat: @"invoking an object of type %@ is not supported", [f class]]);
}
-(id)copyWithZone:(NSZone *)zone {
  return [[List alloc] initWithValue: self.value];
}
-(BOOL)isEqual:(id)object {
  if (self == object)
    return YES;
  if (![object isKindOfClass: [List class]])
    return NO;
  return [self.value isEqual:((List*) object).value];
}
-(NSUInteger) hash {
  return [self.value hash];
}
-(NSString *)description {
  return [self.value description];
}
@end

@implementation Dict
-(id)initWithValue:(NSDictionary *)val {
  self = [super init];
  if (self) {
    self.value = [val mutableCopy];
  }
  return self;
}
-(id)copyWithZone:(NSZone *)zone {
  return [[Dict alloc] initWithValue: self.value];
}
-(Obj*)xlookupVariable:(Str *)name {
  Obj *value = [self.value objectForKey: name];
  if (value == nil) {
    Obj *pkey = [[Str alloc] initWithValue: @"__parent__"];
    Dict *parent = (Dict*) [self.value objectForKey: pkey];
    if (parent != nil) {
      value = [parent xlookupVariable: name];
    }
  }
  return value;
}
-(Obj*)lookupVariable:(Str *)name {
  Obj *value = [self xlookupVariable: name];
  return value == nil ? err([@"Could not find variable with name: " stringByAppendingString: name.value]) : value;
}
-(void)defineVariable:(Str *)name withValue:(Obj *)val {
  [self.value setObject: val forKey: name];
}
-(void)assignVariable:(Str *)name withValue:(Obj *)val {
  if ([self.value objectForKey: name] != nil) {
    [self.value setObject: val forKey: name];
    return;
  }
  Obj *pkey = [[Str alloc] initWithValue: @"__parent__"];
  Dict *parent = (Dict*) [self.value objectForKey: pkey];
  if (parent != nil) {
    [parent assignVariable: name withValue: val];
    return;
  }
  err([@"Tried to assign to undefiend variable: " stringByAppendingString: name.value]);
}
-(BOOL)isEqual:(id)object {
  if (self == object)
    return YES;
  if (![object isKindOfClass: [Dict class]])
    return NO;
  return [self.value isEqual:((Dict*) object).value];
}
-(NSUInteger) hash {
  return [self.value hash];
}
-(NSString *)description {
  return [self.value description];
}
@end

@implementation Macro
-(id)initWithValue: (Obj *(^)(Dict *, NSArray *))val; {
  self = [super init];
  if (self) {
    self.value = val;
  }
  return self;
}
@end

@implementation Function
-(id)initWithValue: (Obj *(^)(NSArray *))val; {
  self = [super init];
  if (self) {
    self.value = val;
  }
  return self;
}
@end

Dict *getRootContext() {
  Dict *rctx = [[Dict alloc] initWithValue: @{}];
  [rctx defineVariable: [[Str alloc] initWithValue: @"quote"] withValue:[[Macro alloc] initWithValue:^Obj*(Dict *ctx, NSArray* args) {
    return [args objectAtIndex: 0];
  }]];
  [rctx defineVariable: [[Str alloc] initWithValue: @"log"] withValue:[[Function alloc] initWithValue:^Obj*(NSArray* args) {
    NSLog(@"%@", [args objectAtIndex: 0]);
    return [[Num alloc] initWithValue: @0];
  }]];
  return rctx;
}

List *parse(NSString *s) {
  List *result = xparse(s);
  if (result == nil)
    err(@"Parser error.");
  return result;
}

List *xparse(NSString *s) {
  return nil;
}

// main
int main(int argc, char **argv) {
  @autoreleasepool {
    [[Obj alloc] init];
    [[Num alloc] initWithValue: @10];
    NSLog(@"xxx %@\n", @[@1, @2, @3]);
    NSLog(@"xxx %@\n", @{@1: @2});
    [[[List alloc] initWithValue: @[
      [[Str alloc] initWithValue: @"log"],
        [[List alloc] initWithValue: @[
          [[Str alloc] initWithValue: @"quote"],
          [[Str alloc] initWithValue: @"hi"]
        ]]
      ]] evalWithContext: getRootContext()];
  }
}
