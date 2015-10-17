/* gcc -std=c89 -framework Foundation -lobjc -Wall -Werror -Wpedantic vm.m */
/* header */
#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>

#import <Foundation/Foundation.h>

extern NSMutableDictionary *ROOT;

void init();
NSMutableDictionary *makeChildContext(NSMutableDictionary *parent);
id parse(NSString *s);
id eval(NSMutableDictionary *ctx, id node);

/* implementation */

NSMutableDictionary *ROOT = nil;

NSMutableDictionary *makeChildContext(NSMutableDictionary *parent) {
  return [@{@"__parent__": parent} mutableCopy];
}

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

void init() {
  if (ROOT != nil)
    return;

  ROOT = [@{
    @"begin": ^id(NSMutableDictionary *ctx, NSArray *args) {
      int len = [args count], i;
      id last = nil;

      for (i = 0; i < len; i++)
        last = eval(ctx, [args objectAtIndex: i]);

      return last;
    },
    @"quote": ^id(NSMutableDictionary *ctx, NSArray *args) {
      return [args objectAtIndex: 0];
    },
    @"print": ^id(NSMutableDictionary *ctx, NSArray *args) {
      NSLog(@"%@", eval(ctx, [args objectAtIndex: 0]));
      return nil;
    },
    @"add": ^id(NSMutableDictionary *ctx, NSArray *args) {
      id a = [args objectAtIndex: 0], b = [args objectAtIndex: 1];

      if ([a isKindOfClass: [NSNumber class]] && [b isKindOfClass: [NSNumber class]])
        return [NSNumber numberWithDouble: [a doubleValue] + [b doubleValue]];

      [NSException raise:@"Cannot add objects of types" format:@"%@ and %@", [a class], [b class]];
      return nil;
    }
  } mutableCopy];
}

id eval(NSMutableDictionary *ctx, id node) {

  if ([node isKindOfClass: [NSNumber class]])
    return node;

  if ([node isKindOfClass: [NSString class]])
    return lookup(ctx, (NSString*) node);

  if ([node isKindOfClass: [NSArray class]]) {
    NSArray *n = node;
    id (^f)(NSMutableDictionary*, NSArray*) = eval(ctx, [n objectAtIndex: 0]);
    NSMutableArray *args = [[NSMutableArray alloc] init];
    int len = [n count], i;

    for (i = 1; i < len; i++)
      [args addObject: [n objectAtIndex: i]];

    return f(ctx, args);
  }

  [NSException raise:@"Can't eval an object of type" format: @"%@", [node class]];
  return nil;
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

  return [@[@"begin"] arrayByAddingObjectsFromArray: [stack lastObject]];
}

/* main */

int main(int argc, char **argv) {
  if (argc != 2) {
    fprintf(stderr, "Usage: %s <file-name>\n", argv[0]);
    return 1;
  }
  @autoreleasepool {
    NSString *path = [NSString stringWithUTF8String: argv[1]];
    NSError *error = nil;
    NSString *source = [NSString stringWithContentsOfFile: path encoding: NSUTF8StringEncoding error:&error];
    id node;

    if (error) {
      fprintf(stderr, "Could not read file %s", argv[1]);
      return 1;
    }
    node = parse(source);
    init();
    eval(ROOT, node);
  }
  return 0;
}
