/* gcc -o output -Wall -std=c99 source.m -framework Foundation -lobjc */

// header
#import <Foundation/Foundation.h>

static int objcnt = 0;

@interface Obj : NSObject {}
@end

@interface Num : Obj
@property (retain) NSNumber *value;
-(id)initWithValue:(NSNumber *)val;
@end

@interface Str : Obj
@property (retain) NSString *value;
-(id)initWithValue:(NSString *)val;
@end

@interface List : Obj
@property (retain) NSMutableArray *value;
-(id)initWithValue:(NSMutableArray *)val;
@end

@interface Dict : Obj
@property (retain) NSMutableDictionary *value;
-(id)initWithValue:(NSMutableArray *)val;
@end

// implementation

@implementation Obj

+ (void)initialize {
  if (self == [Obj class]) {
  }
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
@end

@implementation Str
-(id)initWithValue:(NSString *)val {
  self = [super init];
  if (self) {
    self.value = val;
  }
  return self;
}
@end

@implementation List
-(id)initWithValue:(NSMutableArray *)val {
  self = [super init];
  if (self) {
    self.value = val;
  }
  return self;
}
@end

@implementation Dict
-(id)initWithValue:(NSMutableDictionary *)val {
  self = [super init];
  if (self) {
    self.value = val;
  }
  return self;
}
@end

// main
int main(int argc, char **argv) {
  @autoreleasepool {
    [[Obj alloc] init];
    [[Num alloc] initWithValue: @10];
    NSLog(@"xxx %@\n", @[@1, @2, @3]);
    NSLog(@"xxx %@\n", @{@1: @2});
  }
}
