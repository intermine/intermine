#!/bin/bash

# Build and deploy the testmodel webapp
# This script requires the standard InterMine dependencies:
#  * psql (createdb, psql) - your user should have a postgres
#    role with password authentication set up.

set -euo pipefail # Errors are fatal.

if [ "$#" != "1" ]; then
   echo "Usage: $0 <workspace_dir>"
   exit 2
fi

WORKSPACE_DIR=$1
TESTMINE_DIR=${WORKSPACE_DIR}/testmine
USERPROFILEDB=userprofile-demo
PRODDB=intermine-demo
IMDIR=$HOME/.intermine
PROP_FILE=$IMDIR/testmodel.properties

# Inherit SERVER, PORT, PSQL_USER, PSQL_PWD, TOMCAT_USER and TOMCAT_PWD if in env.
SERVER=${SERVER:-localhost}
PORT=${PORT:-8080}
PSQL_USER=${PSQL_USER:-$USER}
PSQL_PWD=${PSQL_PWD:-$USER}
TOMCAT_USER=${TOMCAT_USER:-manager}
TOMCAT_PWD=${TOMCAT_PWD:-manager}

DEBUG=${DEBUG:-0}

for dep in psql createdb; do
  if test -z "$(which $dep)"; then
    echo "ERROR: $dep not found - please make sure $dep is installed and configured correctly"
    exit 1
  fi
done

if [ "$DEBUG" -eq 1 ]; then
  echo "------> CONFIGURATION SETTINGS:"
  echo "# $IMDIR       = $IMDIR"
  echo "# $SERVER      = $SERVER"
  echo "# $PORT        = $PORT"
  echo "# $PSQL_USER   = $PSQL_USER"
  echo "# $PSQL_PWD    = $PSQL_PWD"
fi

cd "$HOME"

if test ! -d "$IMDIR"; then
    echo Making .intermine configuration directory.
    mkdir "$IMDIR"
fi

echo "------> Checking config..."
if test ! -f "$PROP_FILE"; then
    echo "-- $PROP_FILE not found. Providing default properties file."
    cd "$IMDIR"
    cp "$TESTMINE_DIR"/dbmodel/resources/testmodel.properties "$PROP_FILE"
    sed -i=bak -e "s/PSQL_USER/$PSQL_USER/g" "$PROP_FILE"
    sed -i=bak -e "s/PSQL_PWD/$PSQL_PWD/g" "$PROP_FILE"
    sed -i=bak -e "s/USERPROFILEDB/$USERPROFILEDB/g" "$PROP_FILE"
    sed -i=bak -e "s/PRODDB/$PRODDB/g" "$PROP_FILE"
    sed -i=bak -e "s/SERVER/$SERVER/g" "$PROP_FILE"
    sed -i=bak -e "s/8080/$PORT/g" "$PROP_FILE"
    sed -i=bak -e "s/USER/$USER/g" "$PROP_FILE"
fi

echo "------> Creating databases..."
for db in $USERPROFILEDB $PRODDB; do
    sudo -E -u postgres dropdb -h "$PSQL_HOST" --if-exists ${db}
    sudo -E -u postgres createdb -h "$PSQL_HOST" ${db}
done

##########
## Solr ##
##########



# This is the first point at which we need to refer to the InterMine jars previous built
# So we need to install them to Maven so that the testmine Gradle can fetch them
echo "------> Installing InterMine Gradle project JARs to local Maven..."
cd "${WORKSPACE_DIR}"/intermine
(cd "${WORKSPACE_DIR}"/plugin && ./gradlew install --no-daemon)
./gradlew install  --no-daemon

echo "------> Loading demo data set..."
cd "${TESTMINE_DIR}"

echo "------> Running ./gradlew clean (just in case you ran this before and made a mistake)"
./gradlew clean --stacktrace --no-daemon

echo "------> Running ./gradlew loadsadata"
./gradlew loadsadata --stacktrace --no-daemon

echo "------> Building search index..."
echo "------> (this step will fail if indexes not created -- see config/travis/init-solr.sh) "
./gradlew createSearchIndex --stacktrace --no-daemon

echo "------> Loading userprofile..."
./gradlew insertUserData --stacktrace --no-daemon

echo "------> Running webapp"
echo "------> Running ./gradlew cargoRunLocal"
./gradlew cargoRunLocal --no-daemon &
echo "------> Finished"
