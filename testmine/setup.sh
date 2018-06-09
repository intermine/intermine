#!/bin/bash

# Build and deploy the testmodel webapp
# This script requires the standard InterMine dependencies:
#  * psql (createdb, psql) - your user should have a postgres
#    role with password authentication set up.
#  * ant
#  * a deployment container (tomcat) - see config/download_and_configure_tomcat.sh

set -e # Errors are fatal.

USERPROFILEDB=userprofile-demo
PRODDB=intermine-demo
MINENAME=demomine
DIR="$(cd $(dirname "$0"); pwd)"
IMDIR=$HOME/.intermine
LOG=$DIR/build.log
PROP_FILE=$IMDIR/testmodel.properties

# Inherit SERVER, PORT, PSQL_USER, PSQL_PWD, TOMCAT_USER and TOMCAT_PWD if in env.
if test -z $SERVER; then
    SERVER=localhost
fi
if test -z $PORT; then
    PORT=8080
fi
if test -z $PSQL_USER; then
    PSQL_USER=$USER
fi
if test -z $PSQL_PWD; then
    PSQL_PWD=$USER;
fi
if test -z $TOMCAT_USER; then
    TOMCAT_USER=manager
fi
if test -z $TOMCAT_PWD; then
    TOMCAT_PWD=manager
fi

for dep in psql createdb ant; do
  if test -z $(which $dep); then
    echo "ERROR: $dep not found - please make sure $dep is installed and configured correctly"
    exit 1
  fi
done

if test ! -z $DEBUG; then
  echo "------> CONFIGURATION SETTINGS:"
  echo '# $IMDIR       = '$IMDIR
  echo '# $SERVER      = '$SERVER
  echo '# $PORT        = '$PORT
  echo '# $PSQL_USER   = '$PSQL_USER
  echo '# $PSQL_PWD    = '$PSQL_PWD
  echo '# $TOMCAT_USER = '$TOMCAT_USER
  echo '# $TOMCAT_PWD  = '$TOMCAT_PWD
fi

cd $HOME

if test ! -d $IMDIR; then
    echo Making .intermine configuration directory.
    mkdir $IMDIR
fi

echo "------> Checking config..."
if test ! -f $PROP_FILE; then
    echo "-- $PROP_FILE not found. Providing default properties file."
    cd $IMDIR
    cp $DIR/dbmodel/resources/testmodel.properties $PROP_FILE
    sed -i=bak -e "s/PSQL_USER/$PSQL_USER/g" $PROP_FILE
    sed -i=bak -e "s/PSQL_PWD/$PSQL_PWD/g" $PROP_FILE
    sed -i=bak -e "s/TOMCAT_USER/$TOMCAT_USER/g" $PROP_FILE
    sed -i=bak -e "s/TOMCAT_PWD/$TOMCAT_PWD/g" $PROP_FILE
    sed -i=bak -e "s/USERPROFILEDB/$USERPROFILEDB/g" $PROP_FILE
    sed -i=bak -e "s/PRODDB/$PRODDB/g" $PROP_FILE
    sed -i=bak -e "s/SERVER/$SERVER/g" $PROP_FILE
    sed -i=bak -e "s/8080/$PORT/g" $PROP_FILE
    sed -i=bak -e "s/USER/$USER/g" $PROP_FILE
fi

echo "------> Checking databases..."
for db in $USERPROFILEDB $PRODDB; do
    if psql --list | egrep -q '\s'$db'\s'; then
        echo $db exists.
    else
        echo Creating $db
        createdb $db
    fi
done

# This is the first point at which we need to refer to the InterMine jars previous built
# So we need to install them to Maven so that the testmine Gradle can fetch them
echo "-----> Installing InterMine Gradle project JARs to local Maven..."
cd $DIR/../intermine
(cd ../plugin && ./gradlew install)
./gradlew install

echo "------> Loading demo data set..."
cd $DIR

echo "-----> Running ./gradlew loadsadata"
./gradlew loadsadata

echo "------> Loading userprofile..."
./gradlew insertUserData

echo "------> Running webapp"
echo "-----> Running ./gradlew tomcatstartwar"
./gradlew tomcatstartwar & 
echo "-----> Finished init-webapp.sh"
