#!/bin/bash

set -e

if [ -z $(which wget) ]; then
    # use curl
    GET='curl'
else
    GET='wget -O -'
fi

GIT_GET="git clone --single-branch --depth 1"

export PSQL_USER=postgres
export KEYSTORE=${PWD}/keystore.jks

if [ "$TEST_SUITE" = "checkstyle" ]; then
    exit 0 # nothing to do
else
    # Create all databases we might need.
    psql -c 'create database notxmltest;' -U postgres
    psql -c 'create database truncunittest;' -U postgres
    psql -c 'create database flatmodetest;' -U postgres
    psql -c 'create database fulldatatest;' -U postgres
    psql -c 'create database userprofiletest;' -U postgres
    psql -c 'create database unittest;' -U postgres
    psql -c 'create database biotest;' -U postgres
    psql -c 'create database biofulldatatest;' -U postgres

    # Set up properties
    source config/create-ci-properties-files.sh

    # Initialise a python virtual environment
    virtualenv venv
    source venv/bin/activate

    # Install lib requirements
    pip install -r config/lib/requirements.txt

    # Build resources we might require
    if [ "$TEST_SUITE" = "selenium" ]; then
        # We will need python requirements for selenium tests
        pip install -r testmodel/webapp/selenium/requirements.txt
    fi

    if [ "$TEST_SUITE" = "selenium" -o "$TEST_SUITE" = "ws-integration" ]; then
        # We will need a fully operational web-application
        source config/init-webapp.sh
        source config/issue-token.sh
    elif [ "$TEST_SUITE" = "bio" ]; then
        # Bio requires the bio model
        ant -f bio/test-all/dbmodel/build.xml build-db
    elif [ "$TEST_SUITE" = "api" -o "$TEST_SUITE" = "web" ]; then
        # api and webapp need the testmodel to be built
        ant -f testmodel/dbmodel/build.xml build-db
    fi

    if [[ "$TEST_SUITE" = "ws-integration" ]]; then

        # Warm up the keyword search by requesting results, but ignoring the results
        $GET "$TESTMODEL_URL/service/search" > /dev/null
        # Start any list upgrades
        $GET "$TESTMODEL_URL/service/lists?token=test-user-token" > /dev/null

        if [[ "$CLIENT" = "JS" ]]; then
            # We need the imjs code to exercise the webservices
            $GIT_GET https://github.com/intermine/imjs.git client
        elif [[ "$CLIENT" = "PY" ]]; then
            $GIT_GET https://github.com/intermine/intermine-ws-client.py client
        fi
    fi
fi
