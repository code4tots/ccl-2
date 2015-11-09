# TODO: I haven't written one line of this yet,
# and I know this is gonna be full of crap.
# Clean up when I get a chance.
import os.path

from parse import Parser

class Transpiler(object):

  def transpile(filespec, string):
    self.init(filespec, string)

  def init(self, filespec, string):
    self.parsed_module = Parser().parse(filespec, string)
    self.basename = os.path.basename(filespec)
    if self.basename.endswith('.ccl'):
      self.basename = self.basename[:-4]
    self.modulename = 'ccl_module_' + self.basename
    return self

  def generate_header(self):
    header = (
        '#ifndef %s\n'
        '#define %s\n'
        '#include "ccl.h"\n\n') % (self.modulename, self.modulename)
    for cls in self.parsed_module['classes']:
      name = cls['name']
      header += '#define CCL_Class_%s (&CCL_s_Class_%s)\n' % (name, name)
      header += 'extern CCL_Class CCL_s_Class_%s;\n\n' % name
    header += '#endif/*%s*/\n' % self.modulename
    return header

  def generate_source(self):
    source = '#include "%s.h\n' % self.modulename
    for inc in self.parsed_module['includes']:
      source += '#include "ccl_module_%s.h"\n' % inc['name']
    for cls in self.parsed_module['classes']:
      mds = []
      for md in cls['methods']:
        mds.append(md['name'])
        source += (
            '\nstatic CCL_Object *method_%s_%s(CCL_Object *CCL_self, int argc, CCL_Object **argv) {\n'
            '%s'
            '  CCL_expect_argument_size(%d, argc);\n'
            '%s'
            '%s\n'
            '  return CCL_nil;\n'
            '}\n'
        ) % (
            cls['name'], md['name'],
            ''.join('  CCL_Object *CCL_var_%s;\n' % a for a in md['args']),
            len(md['args']),
            ''.join('  CCL_var_%s = argv[%d];\n' % (a, i) for i, a in enumerate(md['args'])),
            md['body'].replace('\n', '\n  '),
        )
      if cls['methods']:
        source += (
            '\nstatic const CCL_Method methods_%s[] = {\n'
            '%s'
            '};\n'
        ) % (
            cls['name'],
            ''.join(
                '  {"%s", &method_%s_%s},\n' % (md['name'], cls['name'], md['name']) for md in cls['methods']
            )
        )
        methods_entry = '  sizeof(methods_%s)/sizeof(CCL_Method), methods_%s,\n' % (
            cls['name'], cls['name'])
      else:
        methods_entry = '  0, NULL, /* no direct methods */\n'

      source += (
          '\nstatic CCL_Class *bases_%s[] = {\n'
          '%s'
          '};\n'
      ) % (
          cls['name'],
          ''.join('  CCL_Class_%s,\n' % c for c in cls['bases']),
      )

      if cls['attrs']:
        source += (
            '\nstatic const char *attrs_%s[] = {\n'
            '%s'
            '};\n'
        ) % (
            cls['name'],
            ''.join('  "%s",\n' % a for a in cls['attrs']))
        attrs_entry = '  sizeof(attrs_%s)/sizeof(char*), attrs_%s\n' % (cls['name'], cls['name'])
      else:
        attrs_entry = '  0, NULL, /* no direct attributes */\n'

      source += (
          '\nCCL_Class CCL_s_Class_%s = {\n'
          '  "%s",\n'
          '  sizeof(bases_%s)/sizeof(CCL_Class_*), bases_%s,\n'
          '%s'
          '%s'
          '  CCL_CLASS_TYPE_%s\n'
          '  NULL, /* no builtin_constructor */\n'
          '  CCL_CLASS_EPILOGUE\n'
          '};\n'
      ) % (
          cls['name'],
          cls['name'],
          cls['name'], cls['name'],
          attrs_entry,
          methods_entry,
          'SINGLETON' if cls['singleton'] else 'DEFAULT',
      )

    return source

piler = Transpiler().init('blargodir/blargoblarg.ccl', r"""
include other_module

class Blah
  var j k

class Foo
  def method(a, b)
    pass

  def another_method(x, y)
    return true

singleton Bar
  pass

""")

assert piler.generate_header() == r"""#ifndef ccl_module_blargoblarg
#define ccl_module_blargoblarg
#include "ccl.h"

#define CCL_Class_Blah (&CCL_s_Class_Blah)
extern CCL_Class CCL_s_Class_Blah;

#define CCL_Class_Foo (&CCL_s_Class_Foo)
extern CCL_Class CCL_s_Class_Foo;

#define CCL_Class_Bar (&CCL_s_Class_Bar)
extern CCL_Class CCL_s_Class_Bar;

#endif/*ccl_module_blargoblarg*/
""", piler.generate_header()

assert piler.generate_source() == r"""#include "ccl_module_blargoblarg.h
#include "ccl_module_other_module.h"

static CCL_Class *bases_Blah[] = {
  CCL_Class_Object,
};

static const char *attrs_Blah[] = {
  "j",
  "k",
};

CCL_Class CCL_s_Class_Blah = {
  "Blah",
  sizeof(bases_Blah)/sizeof(CCL_Class_*), bases_Blah,
  sizeof(attrs_Blah)/sizeof(char*), attrs_Blah
  0, NULL, /* no direct methods */
  CCL_CLASS_TYPE_DEFAULT
  NULL, /* no builtin_constructor */
  CCL_CLASS_EPILOGUE
};

static CCL_Object *method_Foo_method(CCL_Object *CCL_self, int argc, CCL_Object **argv) {
  CCL_Object *CCL_var_a;
  CCL_Object *CCL_var_b;
  CCL_expect_argument_size(2, argc);
  CCL_var_a = argv[0];
  CCL_var_b = argv[1];

  {
  }
  return CCL_nil;
}

static CCL_Object *method_Foo_another_method(CCL_Object *CCL_self, int argc, CCL_Object **argv) {
  CCL_Object *CCL_var_x;
  CCL_Object *CCL_var_y;
  CCL_expect_argument_size(2, argc);
  CCL_var_x = argv[0];
  CCL_var_y = argv[1];

  {
    return CCL_true;
  }
  return CCL_nil;
}

static const CCL_Method methods_Foo[] = {
  {"method", &method_Foo_method},
  {"another_method", &method_Foo_another_method},
};

static CCL_Class *bases_Foo[] = {
  CCL_Class_Object,
};

CCL_Class CCL_s_Class_Foo = {
  "Foo",
  sizeof(bases_Foo)/sizeof(CCL_Class_*), bases_Foo,
  0, NULL, /* no direct attributes */
  sizeof(methods_Foo)/sizeof(CCL_Method), methods_Foo,
  CCL_CLASS_TYPE_DEFAULT
  NULL, /* no builtin_constructor */
  CCL_CLASS_EPILOGUE
};

static CCL_Class *bases_Bar[] = {
  CCL_Class_Object,
};

CCL_Class CCL_s_Class_Bar = {
  "Bar",
  sizeof(bases_Bar)/sizeof(CCL_Class_*), bases_Bar,
  0, NULL, /* no direct attributes */
  0, NULL, /* no direct methods */
  CCL_CLASS_TYPE_SINGLETON
  NULL, /* no builtin_constructor */
  CCL_CLASS_EPILOGUE
};
""", piler.generate_source()
