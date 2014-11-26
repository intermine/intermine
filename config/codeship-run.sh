#!/bin/bash

set -e

PROJECT=intermine/all

ant -f ${PROJECT}/build.xml clean fulltest checkstyle

./config/lib/parse_test_report.py "${PROJECT}/build/test/results"
./config/lib/parse_checkstyle_report.py "${PROJECT}/build/checkstyle/checkstyle_report.xml"

PSQL_USER=$PG_USER PSQL_PWD=$PG_PASSWORD sh testmodel/setup.sh
sleep 10 # Wait for the webapp to come online

./config/run-selenium-tests.sh
