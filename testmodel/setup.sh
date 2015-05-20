#!/bin/bash

# Build and deploy the testmodel webapp
# This script requires the standard InterMine dependencies:
#  * psql (createdb, psql) - your user should have a postgres
#    role with password authentication set up.
#  * ant
#  * a deployment container (tomcat) - see config/download_and_configure_tomcat.sh

set -e # Errors are fatal.

set -e

USERPROFILEDB=userprofile-demo
PRODDB=objectstore-demo
MINENAME=demomine
DIR="$(cd $(dirname "$0"); pwd)"
IMDIR=$HOME/.intermine
LOG=$DIR/build.log
PROP_FILE=$IMDIR/testmodel.properties.demo
alias ant="ant -Drelease=demo"

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

echo "------> Checking configuration..."
if test ! -d $IMDIR; then
    echo Making .intermine configuration directory.
    mkdir $IMDIR
fi

echo "------> Checking config..."
if test ! -f $PROP_FILE; then
    echo "-- $PROP_FILE not found. Providing default properties file."
    cd $IMDIR
    cp $DIR/testmodel.properties $PROP_FILE
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

echo "------> Checking properties file"
if test ! -d .intermine; then
    mkdir .intermine
    cp $DIR/testmodel.properties .
    sed -i "s/USER/$USER/g" testmodel.properties
fi

echo "------> Checking databases..."
for db in $PRODDB $USERPROFILEDB; do
    if psql --list | egrep -q '\s'$db'\s'; then
        echo $db exists
    else
        echo Creating $db
        createdb $db
    fi
done

echo "------> Processing books..."
cd $DIR/dbmodel/extra/books
make 

echo "------> Checking databases..."
for db in $USERPROFILEDB $PRODDB; do
    if psql --list | egrep -q '\s'$db'\s'; then
        echo $db exists.
    else
        echo Creating $db
        createdb $db
    fi
done

echo "------> Removing current webapp"
cd $DIR/webapp/main
ant -Ddont.minify=true remove-webapp >> $DIR/setup.log

echo "------> Beginning build - logging to $LOG"

echo "------> Processing data sources."
cd $DIR/dbmodel/extra/books
make 

echo "------> Loading demo data set - this should take about 3-4 minutes."
cd $DIR/dbmodel
TASKS="clean load-workers-and-books"
if test ! -z $EXTRA_DATA; then
    MEGACORP_XML="resources/testmodel_mega_data.xml"
    if test ! -f $MEGACORP_XML; then
        echo "-----> Generating mega-corp"
        COMPANY=Mega perl \
            "$DIR/../intermine/objectstore/test/scripts/create_enormo_corp.pl" \
            "$DIR/../intermine/objectstore/model/testmodel/testmodel_model.xml" \
            $MEGACORP_XML
    fi
    TASKS="$TASKS enormocorp megacorp"
fi
ant -Ddont.minify=true -Drelease=demo -v $TASKS >> $LOG

cd $DIR/webapp/main

echo "------> Building and releasing web-app..., nearly done"
ant -Drelease=demo -Ddont.minify=true \
    build-test-userprofile-withuser \
    create-quicksearch-index \
    retrieve-objectstore-summary \
    default \
    release-webapp | tee -a $LOG | grep tomcat-deploy

echo "------> All done. Build log is available in $LOG"
