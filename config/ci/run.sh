#!/bin/bash

set -euo pipefail

if [ "$#" != "5" ]; then
   echo "Usage: $0 <workspace_dir> <python executable> <test_suite> <client> <testmodel_url>"
   exit 1
fi

WORKSPACE_DIR=$1
PYTHON=$2
TEST_SUITE=$3
CLIENT=$4
TESTMODEL_URL=$5

echo "RUNNING test suite $TEST_SUITE"

export ANT_OPTS='-server'

if [ "$TEST_SUITE" = "intermine" ]; then
    echo "RUNNING intermine unit tests"
    (cd "${WORKSPACE_DIR}"/plugin && ./gradlew install)
    # Add --rerun-tasks below to force rebuild
    (cd "${WORKSPACE_DIR}"/intermine && ./gradlew build --info --stacktrace)

    echo CHECKING results
    "${WORKSPACE_DIR}"/config/lib/parse_test_report.py "${WORKSPACE_DIR}/intermine"

    echo ALL TESTS PASSED
elif [ "$TEST_SUITE" = "bio" ]; then
    echo "RUNNING bio unit tests"
    (cd "${WORKSPACE_DIR}"/plugin && ./gradlew install)
    (cd "${WORKSPACE_DIR}"/intermine && ./gradlew install)
    (cd "${WORKSPACE_DIR}"/bio && ./gradlew install)
    (cd "${WORKSPACE_DIR}"/bio/sources && ./gradlew install)
    (cd "${WORKSPACE_DIR}"/bio/postprocess && ./gradlew install)

    (cd "${WORKSPACE_DIR}"/bio && ./gradlew build)
    (cd "${WORKSPACE_DIR}"/bio/sources && ./gradlew build)
    (cd "${WORKSPACE_DIR}"/bio/postprocess && ./gradlew build)
    (cd "${WORKSPACE_DIR}"/bio/postprocess-test && ./gradlew build)

    echo CHECKING results
    "${WORKSPACE_DIR}"/config/lib/parse_test_report.py "${WORKSPACE_DIR}/bio"
    "${WORKSPACE_DIR}"/config/lib/parse_test_report.py "${WORKSPACE_DIR}/bio/sources"
    "${WORKSPACE_DIR}"/config/lib/parse_test_report.py "${WORKSPACE_DIR}/bio/postprocess"
    "${WORKSPACE_DIR}"/config/lib/parse_test_report.py "${WORKSPACE_DIR}/bio/postprocess-test"

    echo ALL TESTS PASSED
elif [ "$TEST_SUITE" = "checkstyle" ]; then
    (cd intermine && ./gradlew checkstyleMain)
    "${WORKSPACE_DIR}"/config/lib/parse_checkstyle_report.py "${WORKSPACE_DIR}/intermine/model/build/reports/checkstyle/main.xml"
    "${WORKSPACE_DIR}"/config/lib/parse_checkstyle_report.py "${WORKSPACE_DIR}/intermine/objectstore/build/reports/checkstyle/main.xml"
    "${WORKSPACE_DIR}"/config/lib/parse_checkstyle_report.py "${WORKSPACE_DIR}/intermine/pathquery/build/reports/checkstyle/main.xml"
    "${WORKSPACE_DIR}"/config/lib/parse_checkstyle_report.py "${WORKSPACE_DIR}/intermine/integrate/build/reports/checkstyle/main.xml"
    "${WORKSPACE_DIR}"/config/lib/parse_checkstyle_report.py "${WORKSPACE_DIR}/intermine/api/build/reports/checkstyle/main.xml"
    "${WORKSPACE_DIR}"/config/lib/parse_checkstyle_report.py "${WORKSPACE_DIR}/intermine/webapp/build/reports/checkstyle/main.xml"
    "${WORKSPACE_DIR}"/config/lib/parse_checkstyle_report.py "${WORKSPACE_DIR}/intermine/webtasks/build/reports/checkstyle/main.xml"

    #ant -f 'bio/test-all/build.xml' checkstyle
    (cd "${WORKSPACE_DIR}"/bio && ./gradlew checkstyleMain)
    (cd "${WORKSPACE_DIR}"/bio/sources && ./gradlew checkstyleMain)
    (cd "${WORKSPACE_DIR}"/bio/postprocess && ./gradlew checkstyleMain)

    "${WORKSPACE_DIR}"/config/lib/parse_checkstyle_report.py "${WORKSPACE_DIR}/bio/build/reports/checkstyle/checkstyle_report.xml"
    "${WORKSPACE_DIR}"/config/lib/parse_checkstyle_report.py "${WORKSPACE_DIR}/bio/postprocess/build/reports/checkstyle/checkstyle_report.xml"
    "${WORKSPACE_DIR}"/config/lib/parse_checkstyle_report.py "${WORKSPACE_DIR}/bio/sources/build/reports/checkstyle/checkstyle_report.xml"
elif [ "$TEST_SUITE" = "ws" ]; then
    "${WORKSPACE_DIR}"/config/run-ws-tests.sh "${WORKSPACE_DIR}" "${PYTHON}" "${CLIENT}" "${TESTMODEL_URL}"
fi
