#!/bin/bash

set -e

echo "RUNNING test suite $TEST_SUITE"

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
    ./config/lib/parse_test_report.py "$1"
    echo ALL TESTS PASSED
}

if [ "$TEST_SUITE" = "webtasks" ]; then
    ant_test 'intermine/webtasks'
elif [ "$TEST_SUITE" = "intermine" ]; then
    echo "RUNNING intermine unit tests"
    (cd intermine && ./gradlew build)

    echo CHECKING results
    ./config/lib/parse_test_report.py 'intermine'

    echo ALL TESTS PASSED
elif [ "$TEST_SUITE" = "bio" ]; then
    echo "RUNNING bio unit tests"
    (cd intermine && ./gradlew install)
    (cd plugin && ./gradlew install)
    (cd bio && ./gradlew install && ./gradlew build)
    (cd bio/sources && ./gradlew install)
    (cd bio/postprocess && ./gradlew install)

    echo CHECKING results
    ./config/lib/parse_test_report.py 'bio'

    echo ALL TESTS PASSED
elif [ "$TEST_SUITE" = "checkstyle" ]; then
    gradle checkstyle
    ./config/lib/parse_checkstyle_report.py 'intermine/all/build/checkstyle/checkstyle_report.xml'
    #ant -f 'bio/test-all/build.xml' checkstyle
    #./config/lib/parse_checkstyle_report.py 'bio/test-all/build/checkstyle/checkstyle_report.xml'
elif [ "$TEST_SUITE" = "webapp" ]; then
    echo 'Running selenium tests'
    . config/run-selenium-tests.sh
elif [ "$TEST_SUITE" = "ws" ]; then
    . config/run-ws-tests.sh
fi
