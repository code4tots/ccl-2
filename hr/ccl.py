# Given a C program, generate a version that is preceeded with ccl.h and ccl.c
import os.path
import sys

thisdir = os.path.dirname(os.path.realpath(__file__))
basedir = os.path.dirname(thisdir)

cclcpath = os.path.join(basedir, 'ccl.c')
cclhpath = os.path.join(basedir, 'ccl.h')

with open(cclcpath) as f:
  cclc = f.read()

with open(cclhpath) as f:
  cclh = f.read()

prefix = cclc.replace('#include "ccl.h"', cclh)

inputpath = sys.argv[1]
outpath = (inputpath + '\n').replace('.c\n', '.gen.c')

with open(inputpath) as f:
  contents = f.read()

with open(outpath, 'w') as f:
  f.write(prefix + '\n')
  f.write(contents)
