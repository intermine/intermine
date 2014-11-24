#!/bin/bash

set -e

# Create all databases we might need.
psql -c 'create database notxmltest;' -U postgres
psql -c 'create database truncunittest;' -U postgres
psql -c 'create database flatmodetest;' -U postgres
psql -c 'create database fulldatatest;' -U postgres
psql -c 'create database userprofiletest;' -U postgres
psql -c 'create database unittest;' -U postgres
psql -c 'create database biotest;' -U postgres
psql -c 'create database biofulldatatest;' -U postgres

touch failures.list # Allowing us to cat it later.

# Set up properties
mkdir ~/.intermine
cp config/ci.properties ~/.intermine/intermine-test.properties
sed -i 's/PG_USER/postgres/' ~/.intermine/intermine-test.properties
cp ~/.intermine/intermine-test.properties ~/.intermine/testmodel.properties
cp config/ci-bio.properties ~/.intermine/intermine-bio-test.properties
sed -i 's/PG_USER/postgres/' ~/.intermine/intermine-bio-test.properties

gem install travis-artifacts

if [ "$TEST_SUITE" = "selenium" ]; then
    sudo pip install -r testmodel/webapp/selenium/requirements.txt
    PSQL_USER=postgres sh testmodel/setup.sh
    sleep 10 # wait for the webapp to come on line
    ./config/download_and_configure_tomcat.sh
fi

# Build any models we might require.
ant -f intermine/objectstore/model/testmodel/build.xml
ant -f intermine/integrate/model/fulldata/build.xml
ant -f intermine/api/model/userprofile/build.xml
ant -f bio/test-all/dbmodel/build.xml build-db

