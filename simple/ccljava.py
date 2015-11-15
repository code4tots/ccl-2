"""ccljava.py

CCL to Java transpiler.

In order to make translation as simple as possible,
only includes and classes are allowed at the module level.

Furthermore, include statements are ignored.

Usage:
python ccljava.py <java_package_name> <ccl_source_dir> <java_source_dir>

Reads all ccl source in ccl_source_dir, then writes out all the classes as
java code in java_source_dir.

"""

import os
import os.path

from parse import Parser


class Transpiler(object):

  def init(self, java_package_name, ccl_source_dir, java_source_dir):
    self.java_package_name = java_package_name
    self.ccl_source_dir = ccl_source_dir
    self.java_source_dir = java_source_dir
    self.classes = []

  def transpile(self):
    self.load_all_modules()

  def load_all_modules(self):
    for filename in os.listdir(self.ccl_source_dir):
      if filename.endswith('.ccl'):
        path = os.path.abspath(os.path.join(self.ccl_source_dir, filename))
        self.load_module_at(path)

  def load_module_at(self, path_to_module):
    with open(path_to_module) as f:
      ccl_source_code = f.read()
    module = Parser().parse(path_to_module, ccl_source_code)
    if module['funcs']:
      raise SyntaxError(
          'Only classes are allowed, '
          'but functions were found in %r' % path_to_module)
    if module['vars']:
      raise SyntaxError(
          'Only classes are allowed, '
          'but variables were found in %r' % path_to_module)
    if module['stmts']:
      raise SyntaxError(
          'Only classes are allowed, '
          'but statements were found in %r' % path_to_module)
    self.classes.extend(module['classes'])

