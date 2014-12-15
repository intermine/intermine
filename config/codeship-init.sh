#!/bin/bash

# Errors are fatal.
set -e

# Create databases.
psql -c 'create database notxmltest;'
psql -c 'create database truncunittest;'
psql -c 'create database flatmodetest;'
psql -c 'create database fulldatatest;'
psql -c 'create database userprofiletest;'
psql -c 'create database unittest;'
psql -c 'create database biotest;'
psql -c 'create database biofulldatatest;'

# Create property files for running tests.
mkdir $HOME/.intermine
cp config/ci.properties $HOME/.intermine/intermine-test.properties
sed -i "s/PG_USER/$PG_USER/" $HOME/.intermine/intermine-test.properties
sed -i "s/PG_PASS/$PG_PASSWORD/" $HOME/.intermine/intermine-test.properties
cp $HOME/.intermine/intermine-test.properties $HOME/.intermine/testmodel.properties
cp config/ci-bio.properties $HOME/.intermine/intermine-bio-test.properties
sed -i "s/PG_USER/$PG_USER/" $HOME/.intermine/intermine-bio-test.properties
sed -i "s/PG_PASS/$PG_PASSWORD/" $HOME/.intermine/intermine-bio-test.properties

# Install requirements for running selenium test.
pip install -r config/lib/requirements.txt
pip install -r testmodel/webapp/selenium/requirements.txt

# Build models we need.
ant -f testmodel/dbmodel/build.xml build-db
ant -f bio/test-all/dbmodel/build.xml build-db

# Install and configure tomcat 7.0.53
source config/download_and_configure_tomcat.sh
sleep 5 # Wait for tomcat to be available
PSQL_USER=$PG_USER PSQL_PWD=$PG_PASSWORD sh testmodel/setup.sh
sleep 10 # Wait for the webapp to come online
