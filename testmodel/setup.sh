#!/usr/bin/bash

USERPROFILEDB=userprofile-demo
PRODDB=objectstore-demo
DIR="$(cd $(dirname "$0"); pwd)"

cd $HOME

if test ! -d .intermine; then
    mkdir .intermine
    cp $DIR/testmodel.properties .
    sed -i "s/USER/$USER/g" testmodel.properties
fi

if test ! psql --list | egrep -q \b$USERPROFILEDB\b; then
    echo Creating $USERPROFILEDB
    createdb $USERPROFILEDB
fi

if test ! psql --list | egrep -q \b$PRODDB\b; then
    echo Creating $PRODDB
    createdb $PRODDB
fi

cd $DIR/dbmodel

ant loadsadata

cd $DIR/webapp/main

ant -Ddont.minify=true \
    build-test-userprofile-withuser \
    create-quicksearch-index \
    default \
    remove-webapp \
    release-webapp

