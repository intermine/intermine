#!/usr/bin/env python

from __future__ import print_function

import sys
import os
import os.path as path
import xunitparser

directory = sys.argv[1]
failure_count = 0

for filename in os.listdir(directory):
    if filename.endswith('.xml') and not filename.endswith('TestSuites.xml'):
        with open(path.join(directory, filename)) as f:
            suite, tr = xunitparser.parse(f)

            failures = [testcase for testcase in suite if not testcase.good]

            for testcase in failures:
                print('%s: Class %s, method %s' % (testcase.result.upper(), testcase.classname, testcase.methodname))
                print(testcase.trace)

            failure_count += len(failures)

if failure_count:
    print(failure_count, 'TESTS FAILED')
    sys.exit(failure_count)

