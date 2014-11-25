#!/bin/bash

set -e

ant -f intermine/all/build.xml clean fulltest checkstyle

./config/lib/parse_test_report.py "intermine/all/build/test/results"

PSQL_USER=$PG_USER PSQL_PWD=$PG_PASSWORD sh testmodel/setup.sh
sleep 10 # Wait for the webapp to come online

./config/run-selenium-tests.sh
