#!/bin/bash

set -e

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
    if [ "$TEST_SUITE" = "selenium" -o "$TEST_SUITE" = "ws-integration"]; then
        # We will need python requirements for selenium tests
        pip install -r testmodel/webapp/selenium/requirements.txt
        # We need a running webapp
        source config/download_and_configure_tomcat.sh
        sleep 10 # wait for tomcat to come on line
        PSQL_USER=postgres sh testmodel/setup.sh
        sleep 10 # wait for the webapp to come on line
    elif [ "$TEST_SUITE" = "bio" ]; then
        # Bio requires the bio model
        ant -f bio/test-all/dbmodel/build.xml build-db
    elif [ "$TEST_SUITE" = "api" -o "$TEST_SUITE" = "web" ]; then
        # api and webapp need the testmodel to be built
        ant -f testmodel/dbmodel/build.xml build-db
    fi

    if [ "$TEST_SUITE" = "ws-integration"]; then
        # We need the imjs code to exercise the webservices
        git clone https://github.com/intermine/imjs.git imjs
    fi
fi
