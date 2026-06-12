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

setup_postgres() {
    sudo -E -u postgres dropdb -h "$PSQL_HOST" --if-exists flatmodetest
    sudo -E -u postgres dropdb -h "$PSQL_HOST" --if-exists fulldatatest
    sudo -E -u postgres dropdb -h "$PSQL_HOST" --if-exists notxmltest
    sudo -E -u postgres dropdb -h "$PSQL_HOST" --if-exists truncunittest
    sudo -E -u postgres dropdb -h "$PSQL_HOST" --if-exists unittest
    sudo -E -u postgres dropdb -h "$PSQL_HOST" --if-exists userprofile-test
    sudo -E -u postgres dropdb -h "$PSQL_HOST" --if-exists bio-test
    sudo -E -u postgres dropdb -h "$PSQL_HOST" --if-exists bio-fulldata-test
    sudo -E -u postgres dropdb -h "$PSQL_HOST" --if-exists intermine-demo
    sudo -E -u postgres dropdb -h "$PSQL_HOST" --if-exists userprofile-demo

    sudo -E -u postgres dropuser -h "$PSQL_HOST" --if-exists test
    sudo -E -u postgres createuser -h "$PSQL_HOST" test
    sudo -E -u postgres psql -h "$PSQL_HOST" -c "alter user test with encrypted password 'test';"
}

setup_python() {
    echo '#---> Installing python requirements'
    # Install lib requirements
    ${PYTHON} -m pip install -r "${WORKSPACE_DIR}"/config/lib/requirements.txt
}

setup_python

if [[ "$TEST_SUITE" = "intermine" ]]; then
    setup_postgres
fi

if [[ "$TEST_SUITE" = "bio" ]]; then
    setup_postgres
fi

if [[ "$TEST_SUITE" = "ws" ]]; then
    setup_postgres
fi

${PYTHON} "${WORKSPACE_DIR}"/config/lib/install_intermine.py

if [[ "$TEST_SUITE" = "ws" ]]; then
    # set up database for testing
    (cd "${WORKSPACE_DIR}"/intermine && ./gradlew createUnitTestDatabases)

    # We will need a fully operational web-application
    echo '#---> Building and releasing web application to test against'
    (cd "${WORKSPACE_DIR}"/testmine && ./setup.sh "${WORKSPACE_DIR}")

    # Warm up the keyword search by requesting results, but ignoring the results
    $GET "$TESTMODEL_URL/service/search" > /dev/null
    # Start any list upgrades
    $GET "$TESTMODEL_URL/service/lists?token=test-user-token" > /dev/null

    cd "${WORKSPACE_DIR}"
    if [[ "$CLIENT" = "JS" ]]; then
        # We need the imjs code to exercise the webservices
        $GIT_GET https://github.com/intermine/imjs.git client-JS
    elif [[ "$CLIENT" = "PY" ]]; then
        $GIT_GET -b dev https://github.com/intermine/intermine-ws-python client-PY
    fi
fi
