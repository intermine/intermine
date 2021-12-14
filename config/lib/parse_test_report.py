#!/usr/bin/env python

from __future__ import print_function

import sys
import os
import os.path as path
import xunitparser

# This requires the Gradle top-level directory (e.g. 'intermine' or 'bio')
directory = sys.argv[1]
total_failure_count = 0
total_test_count = 0

for project_directory in next(os.walk(directory))[1]:
    tests_path = path.join(directory, project_directory, 'build/test-results/test')

    if not path.isdir(tests_path):
        # print('Ignoring %s as it does not exist' % tests_path)
        continue

    print('Processing %s' % tests_path)

    test_count = 0
    failure_count = 0

    for entry in next(os.walk(tests_path))[2]:
        if entry.endswith('.xml') and not entry.endswith('TestSuites.xml'):
            with open(path.join(tests_path, entry)) as f:
                suite, tr = xunitparser.parse(f)

                failures = [testcase for testcase in suite if testcase and not testcase.good]

                for testcase in failures:
                    print(
                        '%s: Class %s, method %s' % (testcase.result.upper(), testcase.classname, testcase.methodname))
                    print(testcase.trace)

                test_count += len(list(suite))
                failure_count += len(failures)
        # else:
        #     print('Ignoring %s' % entry)

    print('Found %d tests with %d failures' % (test_count, failure_count))

    total_test_count += test_count
    total_failure_count += failure_count

print(total_test_count, 'tests were run')

if total_failure_count:
    print(total_failure_count, 'TESTS FAILED')
    sys.exit(total_failure_count)
