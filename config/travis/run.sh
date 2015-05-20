#!/bin/bash

set -e

export ANT_OPTS='-server'

FAILURES=$PWD/failures.list

ant_test () {
    if [ -d "$1/main" ]; then
      echo RUNNING ant -f "$1/main/build.xml" clean
      ant -f "$1/main/build.xml" clean
      echo RUNNING ant -f "$1/main/build.xml"
      ant -f "$1/main/build.xml"
    fi
    echo RUNNING ant -f "$1/test/build.xml" clean
    ant -f "$1/test/build.xml" clean
    echo RUNNING ant -f "$1/test/build.xml"
    ant -f "$1/test/build.xml" -Ddont.minify=true
    echo CHECKING results
    ./config/lib/parse_test_report.py "$1/test/build/test/results"
    echo ALL TESTS PASSED
}

if [ "$TEST_SUITE" = "model" ]; then
    ant_test 'intermine/model'
elif [ "$TEST_SUITE" = "objectstore" ]; then
    ant_test 'intermine/objectstore'
elif [ "$TEST_SUITE" = "integrate" ]; then
    ant_test 'intermine/integrate'
elif [ "$TEST_SUITE" = "pq" ]; then
    ant_test 'intermine/pathquery'
elif [ "$TEST_SUITE" = "api" ]; then
    ant_test 'intermine/api'
elif [ "$TEST_SUITE" = "web" ]; then
    ant_test 'intermine/web'
elif [ "$TEST_SUITE" = "webtasks" ]; then
    ant_test 'intermine/webtasks'
elif [ "$TEST_SUITE" = "all" ]; then
    echo "RUNNING test-all"
    ant -f "intermine/all/build.xml" fulltest
    echo CHECKING results
    ./config/lib/parse_test_report.py "intermine/all/build/test/results"
    echo ALL TESTS PASSED
elif [ "$TEST_SUITE" = "bio" ]; then
    echo "RUNNING bio tests"
    ant -f 'bio/test-all/build.xml' fulltest
    echo CHECKING results
    ./config/lib/parse_test_report.py "bio/test-all/build/test/results"
    echo ALL TESTS PASSED
elif [ "$TEST_SUITE" = "checkstyle" ]; then
    ant -f 'intermine/all/build.xml' checkstyle
    ./config/lib/parse_checkstyle_report.py 'intermine/all/build/checkstyle/checkstyle_report.xml'
elif [ "$TEST_SUITE" = "checkstyle-bio" ]; then
    ant -f 'bio/test-all/build.xml' checkstyle
    ./config/lib/parse_checkstyle_report.py 'bio/test-all/build/checkstyle/checkstyle_report.xml'
elif [ "$TEST_SUITE" = "webapp" ]; then
    echo 'Running selenium tests'
    . config/run-selenium-tests.sh
elif [ "$TEST_SUITE" = "ws" ]; then
    . config/run-ws-tests.sh
elif [ "$TEST_SUITE" = "bio-webapp" ]; then
    . config/run-bio-webapp-tests.sh
fi
