/* header */
#include <ctype.h>
#include <stdlib.h>

#import <Foundation/Foundation.h>

extern NSMutableDictionary *ROOT;

void init();
NSMutableDictionary *makeChildContext(NSMutableDictionary *parent);
id parse(NSString *s);
id eval(NSMutableDictionary *ctx, id node);

/* implementation */

NSMutableDictionary *ROOT = nil;

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

void init() {
  ROOT = [@{
    @"quote": id^(NSMutableDictionary *ctx, NSArray *args) {

    },
  } mutableCopy];
}

NSMutableDictionary *makeChildContext(NSMutableDictionary *parent) {
  return [@{@"__parent__": parent} mutableCopy];
}

id parse(NSString *string) {
  int i = 0, j, len = string.length, raw;
  const char *s = string.UTF8String;
  NSMutableArray *stack = [[NSMutableArray alloc] init];
  [stack addObject: [[NSMutableArray alloc] init]];

  while (1) {
    while (i < len && isspace(s[i]))
      i++;

    if (i >= len)
      break;

    if (s[i] == '(') {
      [stack addObject: [[NSMutableArray alloc] init]];
      i++;
      continue;
    }

    if (s[i] == ')') {
      NSMutableDictionary *list = [stack lastObject];
      [stack removeLastObject];
      [[stack lastObject] addObject: list];
      i++;
      continue;
    }

    j = i;

    /* number */
    while (s[i] == '-' || s[i] == '.' ||  isdigit(s[i]))
      i++;

    if (j < i) {
      [[stack lastObject] addObject: [NSNumber numberWithDouble: atof(s+j)]];
      continue;
    }

    /* string literal */
    raw = 0;
    if (s[i] == 'r' && i+1 < len)
      i++, raw = 1;
    if (s[i] == '"' || s[i] == '\'') {
      NSMutableString *ms = [[NSMutableString alloc] init];
      char q = s[i];
      i++;

      while (i < len && s[i] != q) {
        if (s[i] == '\\') {
          i++;
          if (i >= len)
            [NSException raise:@"XXX" format:@""];
          switch(s[i]) {
          case 'n': [ms appendFormat: @"\n"]; break;
          case 't': [ms appendFormat: @"\t"]; break;
          default: [NSException raise:@"CCC" format:@""];
          }
        }
        else
          [ms appendFormat: @"%c", s[i]];
        i++;
      }

      if (i >= len)
        [NSException raise:@"Quote fin" format:@""];

      i++;

      [[stack lastObject] addObject: @[@"quote", ms]];

      continue;
    }
    i = j;

    /* name */
    while (i < len && (s[i] == '_' || isalnum(s[i])))
      i++;

    if (j < i) {
      [[stack lastObject] addObject: [string substringWithRange: NSMakeRange(j, i-j)]];
      continue;
    }

    /* err */
    [NSException raise:@"Unrecognized token" format: @"xxx"];
  }

  if ([stack count] != 1)
    [NSException raise:@"Foo" format: @"Bar"];

  return [stack lastObject];
}

/* main */

int main(int argc, char **argv) {
  NSLog(@"%@", @[@1, @2, @3]);
  NSLog(@"\n%@", parse(@"1 2 3 'hi' there"));
  return 0;
}
