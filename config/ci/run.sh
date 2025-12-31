#!/bin/bash

set -euxo pipefail

if [ "$#" != "5" ]; then
   echo "Usage: $0 <workspace_dir> <python executable> <test_suite> <client> <testmodel_url>"
   exit 2
fi

WORKSPACE_DIR=$1
PYTHON=$2
TEST_SUITE=$3
CLIENT=$4
TESTMODEL_URL=$5

echo "RUNNING test suite $TEST_SUITE"

export ANT_OPTS='-server'


run_gradle() {
    local dir=$1
    local command=$2

    (cd "${WORKSPACE_DIR}/$dir" && ./gradlew -Dorg.gradle.jvmargs=-Xmx4g --max-workers=4 --no-daemon  --warning-mode all --stacktrace "$command")
}

gradlew_install() {
    local dir=$1

    run_gradle "$dir" install
}

gradlew_build() {
    # Add --rerun-tasks below to force rebuild
    # Add --info --stacktrace for more useful information when debugging
    # && ./gradlew clean if things look a bit broken (also killall java and rm -r ~/.gradle/caches)

    local dir=$1

    run_gradle "$dir" build
}

if [ "$TEST_SUITE" = "intermine" ]; then
    echo "RUNNING intermine unit tests"
    gradlew_install plugin
    gradlew_build intermine

    echo CHECKING results
    "${WORKSPACE_DIR}"/config/lib/parse_test_report.py "${WORKSPACE_DIR}/intermine"

    echo ALL TESTS PASSED
elif [ "$TEST_SUITE" = "bio" ]; then
    echo "RUNNING bio unit tests"
    gradlew_install plugin
    gradlew_install intermine
    gradlew_install bio
    gradlew_install bio/sources
    gradlew_install bio/postprocess

    gradlew_build bio
    gradlew_build bio/sources
    gradlew_build bio/postprocess
    gradlew_build bio/postprocess-test

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
