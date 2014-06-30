#!/usr/bin/bash
# Build and deploy the testmodel webapp
# This script requires the standard InterMine dependencies:
#  * psql (createdb, psql) - your user should have a postgres
#    role with password authentication set up.
#  * ant
#  * a deployment container (tomcat).

set -e # Errors are fatal.

USERPROFILEDB=userprofile-demo
PRODDB=objectstore-demo
MINENAME=demomine
DIR="$(cd $(dirname "$0"); pwd)"
IMDIR=$HOME/.intermine
LOG=$DIR/build.log
PROP_FILE=$IMDIR/testmodel.properties.demo

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


cd $HOME

if test ! -d $IMDIR; then
    echo Making .intermine configuration directory.
    mkdir $IMDIR
fi

if test ! -f $PROP_FILE; then
    echo $PROP_FILE not found. Providing default properties file.
    cd $IMDIR
    cp $DIR/testmodel.properties $PROP_FILE
    sed -i "s/PSQL_USER/$PSQL_USER/g" $PROP_FILE
    sed -i "s/PSQL_PWD/$PSQL_PWD/g" $PROP_FILE
    sed -i "s/TOMCAT_USER/$TOMCAT_USER/g" $PROP_FILE
    sed -i "s/TOMCAT_PWD/$TOMCAT_PWD/g" $PROP_FILE
    sed -i "s/USERPROFILEDB/$USERPROFILEDB/g" $PROP_FILE
    sed -i "s/PRODDB/$PRODDB/g" $PROP_FILE
    sed -i "s/SERVER/$SERVER/g" $PROP_FILE
    sed -i "s/8080/$PORT/g" $PROP_FILE
    sed -i "s/USER/$USER/g" $PROP_FILE
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

echo "Removing current webapp"
cd $DIR/webapp/main
ant -Drelease=demo -Ddont.minify=true remove-webapp >> $DIR/setup.log

cd $DIR/dbmodel

echo "------> Loading demo data set..."
ant -Drelease=demo loadsadata >> $LOG

cd $DIR/webapp/main

echo "------> Building and releasing web-app..."
ant -Drelease=demo -Ddont.minify=true \
    build-test-userprofile-withuser \
    create-quicksearch-index \
    default \
    release-webapp >> $DIR/setup.log

echo "------> All done. Build log is available in $LOG"

