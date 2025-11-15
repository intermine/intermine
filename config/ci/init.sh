#!/bin/bash

set -euo pipefail

if [ "$#" != "5" ]; then
   echo "Usage: $0 <workspace_dir> <python executable> <test_suite> <client> <testmodel_url>"
   exit 2
fi

WORKSPACE_DIR=$1
PYTHON=$2
TEST_SUITE=$3
CLIENT=$4
TESTMODEL_URL=$5

export PSQL_USER=test
export PSQL_PWD=test
export PSQL_HOST=localhost
export PGPASSWORD=${PGPASSWORD:-postgres}

if [ -z "$(which wget)" ]; then
    # use curl
    GET='curl'
else
    GET='wget -O -'
fi

GIT_GET="git clone --single-branch --depth 1"

export KEYSTORE=${PWD}/keystore.jks

echo "#---> Running $TEST_SUITE tests"

sudo -E -u postgres dropdb -h "$PSQL_HOST" --if-exists flatmodetest
sudo -E -u postgres dropdb -h "$PSQL_HOST" --if-exists fulldatatest
sudo -E -u postgres dropdb -h "$PSQL_HOST" --if-exists notxmltest
sudo -E -u postgres dropdb -h "$PSQL_HOST" --if-exists truncunittest
sudo -E -u postgres dropdb -h "$PSQL_HOST" --if-exists unittest
sudo -E -u postgres dropdb -h "$PSQL_HOST" --if-exists userprofile-test

sudo -E -u postgres dropuser -h "$PSQL_HOST" --if-exists test
sudo -E -u postgres createuser -h "$PSQL_HOST" test
sudo -E -u postgres psql -h "$PSQL_HOST" -c "alter user test with encrypted password 'test';"

if [ "$TEST_SUITE" = "checkstyle" ]; then
    exit 0 # nothing to do
else
    # Set up properties
    source "${WORKSPACE_DIR}"/config/create-ci-properties-files.sh

    echo '#---> Installing python requirements'
    # Install lib requirements
    ${PYTHON} -m pip install -r "${WORKSPACE_DIR}"/config/lib/requirements.txt

    if [[ "$TEST_SUITE" = "ws" ]]; then

        # install everything first. we don't want to test what's in maven
        (cd "${WORKSPACE_DIR}"/plugin && ./gradlew install)
        (cd "${WORKSPACE_DIR}"/intermine && ./gradlew install)
        (cd "${WORKSPACE_DIR}"/bio && ./gradlew install)
        (cd "${WORKSPACE_DIR}"/bio/sources && ./gradlew install)
        (cd "${WORKSPACE_DIR}"/bio/postprocess && ./gradlew install)

        # set up database for testing
        (cd "${WORKSPACE_DIR}"/intermine && ./gradlew createUnitTestDatabases)

        # We will need a fully operational web-application
        echo '#---> Building and releasing web application to test against'
        (cd "${WORKSPACE_DIR}"/testmine && ./setup.sh "${WORKSPACE_DIR}")

        sleep 60 # let webapp startup

        # Warm up the keyword search by requesting results, but ignoring the results
        $GET "$TESTMODEL_URL/service/search" > /dev/null
        # Start any list upgrades
        $GET "$TESTMODEL_URL/service/lists?token=test-user-token" > /dev/null

        cd "${WORKSPACE_DIR}"
        if [[ "$CLIENT" = "JS" ]]; then
            # We need the imjs code to exercise the webservices
            $GIT_GET https://github.com/intermine/imjs.git client
        elif [[ "$CLIENT" = "PY" ]]; then
            $GIT_GET -b python310-dev https://github.com/ucam-department-of-psychiatry/intermine-ws-python client
        fi
    fi
fi
