#!/usr/bin/bash

set -e # Errors are fatal.

USERPROFILEDB=userprofile-demo
# Build and deploy the testmodel webapp
# This script requires the standard InterMine dependencies:
#  * psql (createdb, psql) - your user should have a postgres
#    role with password authentication set up.
#  * ant
#  * a deployment container (tomcat).
PRODDB=objectstore-demo
DIR="$(cd $(dirname "$0"); pwd)"


cd $HOME

if test ! -d .intermine; then
    echo Making .intermine configuration directory.
    mkdir .intermine
fi

if test ! -f $DIR/testmodel.properties; then
    echo No properties found. Providing default properties file.
    cp $DIR/testmodel.properties .
    sed -i "s/USER/$USER/g" testmodel.properties
fi

if psql --list | egrep -q $USERPROFILEDB; then
    echo $USERPROFILEDB exists.
else
    echo Creating $USERPROFILEDB
    createdb $USERPROFILEDB
fi

if psql --list | egrep -q $PRODDB; then
    echo $PRODDB exists.
else
    echo Creating $PRODDB
    createdb $PRODDB
fi

cd $DIR/dbmodel

echo Loading demo data set...
ant loadsadata >> $DIR/setup.log

cd $DIR/webapp/main

echo Building and releasing web-app...
ant -Ddont.minify=true \
    build-test-userprofile-withuser \
    create-quicksearch-index \
    default \
    remove-webapp \
    release-webapp >> $DIR/setup.log

echo All done. Build log is available in $DIR/setup.log

