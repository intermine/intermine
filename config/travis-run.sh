#!/bin/bash


set -e

export ANT_OPTS='-server'

ant_test () {
    ant -f "$1/test/build.xml" clean
    ant -f "$1/test/build.xml" -Ddont.minify=true 2>&1 \
        | tee >(grep FAILED >> failures.list)
}

if [ "$TEST_SUITE" = "model" ]; then
    ant_test 'intermine/model'
elif [ "$TEST_SUITE" = "objectstore" ]; then
    ant_test 'intermine/objectstore'
elif [ "$TEST_SUITE" = "pathquery" ]; then
    ant_test 'intermine/pathquery'
elif [ "$TEST_SUITE" = "api" ]; then
    ant_test 'intermine/api'
elif [ "$TEST_SUITE" = "web" ]; then
    ant_test 'intermine/web'
elif [ "$TEST_SUITE" = "bio" ]; then
    ant_test 'bio/core'
elif [ "$TEST_SUITE" = "checkstyle" ]; then
    ant -f 'intermine/all/build.xml' checkstyle \
        | tee >(grep FAILED >> failures.list)
elif [ "$TEST_SUITE" = "selenium" ]; then
    PSQL_USER=postgres sh testmodel/setup.sh
    sleep 10 # wait for the webapp to come on line
    sh config/run-selenium-tests.sh
fi

cat failures.list
test ! -s failures.list
