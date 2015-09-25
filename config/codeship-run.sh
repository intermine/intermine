#!/bin/bash

PROJECT=intermine/all

ant -f ${PROJECT}/build.xml clean fulltest checkstyle

TESTMODEL_URL='http://localhost:8080/intermine-demo' ./config/run-selenium-tests.sh
SELENIUM_STATUS=$?
./config/lib/parse_test_report.py "${PROJECT}/build/test/results"
ANT_STATUS=$?
./config/lib/parse_checkstyle_report.py "${PROJECT}/build/checkstyle/checkstyle_report.xml"
CHECKSTYLE_STATUS=$?

if [[ $SELENIUM_STATUS > 0 ]] || [[ $ANT_STATUS > 0 ]] || [[ $CHECKSTYLE_STATUS > 0 ]]; then
    echo "BUILD FAILED"
    exit 1
fi

