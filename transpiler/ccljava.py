"""ccljava.py

CCL to Java transpiler.

Translates and dumps all CCL into a single Java 

Usage:
python ccljava.py <java_package_name> <ccl_source_dir> <output_java_filename>

e.g.
python simple/ccljava.py org.ccl cclsrc/ javasrc/Blarg.java

Reads all ccl source in ccl_source_dir, then writes out all the classes as
java code in output_java_filename.

"""

import os
import os.path
import sys

from parse import Parser


class Transpiler(object):

  def init(self, java_package_name, ccl_source_dir, output_java_filename):
    self.java_package_name = java_package_name
    self.ccl_source_dir = ccl_source_dir
    self.output_java_filename = output_java_filename
    self.modules = []
    self.java_class_name = os.path.basename(output_java_filename)
    if self.java_class_name.endswith('.java'):
      self.java_class_name = self.java_class_name[:-len('.java')]
    return self

  def transpile(self):
    self.load_all_modules()
    self.sort_modules()

    source = self.generate_java()
    with open(self.output_java_filename, 'w') as f:
      f.write(source + '\n')

  def load_all_modules(self):
    for filename in os.listdir(self.ccl_source_dir):
      if filename.endswith('.ccl'):
        path = os.path.abspath(os.path.join(self.ccl_source_dir, filename))
        self.load_module_at(path)

  def load_module_at(self, path_to_module):
    with open(path_to_module) as f:
      ccl_source_code = f.read()
    module = Parser().parse(path_to_module, ccl_source_code)
    self.modules.append(module)

  def sort_modules(self):
    # TODO: Use algorithm with better runtime complexity.
    old_modules = self.modules
    self.modules = []

    for module in old_modules:
      i = 0
      while i < len(self.modules):
        if module['name'] in self.modules[i]['includes']:
          break
        i += 1
      self.modules.insert(i, module)

    self.verify_modules_are_properly_ordered()

  def verify_modules_are_properly_ordered(self):
    for i in range(len(self.modules)):
      a = self.modules[i]
      for j in range(i+1, len(self.modules)):
        b = self.modules[j]
        if b['name'] in a['includes']:
          raise SyntaxError(
              'There is a circular dependency issue involving '
              'modules %r and %r' % (a['name'], b['name']))

  def generate_java(self):
    pk = ''
    if self.java_package_name:
      pk = 'package ' + self.java_package_name + ';\n'
    return (
        '%s'
        '\npublic class %s extends ccl.Runtime {'
        '%s'
        '\n  public static void main(String[] args) {'
        '\n    method_main();'
        '\n  }'
        '\n}') % (
            pk,
            self.java_class_name,
            self.transpile_all_modules(),
        )

  def transpile_all_modules(self):
    return ''.join(
        self.transpile_module(m).replace('\n', '\n  ')
        for m in self.modules)

  def transpile_module(self, module):
    return (
        '\npublic static class Module_%s {'
        '\n}'
        '%s') % (
            module['name'],
            ''.join(self.transpile_classes(module['classes'])),
        )

  def transpile_classes(self, classes):
    return ''.join(self.transpile_class(cls) for cls in classes)

  def transpile_class(self, cls):
    if len(cls['bases']) == 0:
      base = 'Object'
    elif len(cls['bases']) == 1:
      base = cls['bases'][0]
    else:
      raise ValueError('Multiple inheritance is not yet supported')

    attrs = ''
    if cls['attrs']:
      attrs = '\n  Class_Object %s;' % ', '.join(
          'var_' + a + ' = nil' for a in cls['attrs'])

    return (
        '\npublic static class Class_%s extends Class_%s {'
        '%s'
        '\n}') % (
            cls['name'], base,
            attrs,
        )

if __name__ == '__main__':
  # TODO: Better error handling
  _, package_name, source_dir, filename = sys.argv
  Transpiler().init(package_name, source_dir, filename).transpile()

