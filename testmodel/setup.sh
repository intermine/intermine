#!/usr/bin/bash

set -e

USERPROFILEDB=userprofile-demo
PRODDB=objectstore-demo
DIR="$(cd $(dirname "$0"); pwd)"
LOG=$DIR/build.log

cd $HOME

echo checking properties file
if test ! -d .intermine; then
    mkdir .intermine
    cp $DIR/testmodel.properties .
    sed -i "s/USER/$USER/g" testmodel.properties
fi

echo checking dbs.
for db in $PRODDB $USERPROFILEDB; do
    if psql --list | egrep -q '\s'$db'\s'; then
        echo $db exists
    else
        echo Creating $db
        createdb $db
    fi
done

echo Beginning build - logging to $LOG

echo Processing data sources.
cd $DIR/dbmodel/extra/books
make 

cd $DIR/dbmodel
echo "Loading test data set - this takes about 3-4 minutes."
ant -v clean load-workers-and-books >> $LOG

cd $DIR/webapp/main

echo "Building and releasing web-application - about 1min left."
ant -Ddont.minify=true \
    build-test-userprofile-withuser \
    create-quicksearch-index \
    default \
    remove-webapp \
    release-webapp | tee $LOG | grep tomcat-deploy

echo build complete. Log available in $LOG

