#!/bin/bash

# InterMine code depends on a number of generated source files. This
# script runs the ant tasks necessary to initialise the projects, making
# it possible to use IDEs, such as eclipse.

set -e

IMDIR=$HOME/.intermine
PROP_FILE=intermine-test.properties
BIO_FILE=intermine-bio-test.properties
DIR="$(cd $(dirname "$0"); pwd)"
LOG=$DIR/../init.log
DBS="unittest truncunittest fulldatatest flatmodetest notxmltest bio-test bio-fulldata-test"
DEFAULT_ANT_OPTS='-server -XX:MaxPermSize=256M -Xmx1700m -XX:+UseParallelGC -Xms1700m -XX:SoftRefLRUPolicyMSPerMB=1 -XX:MaxHeapFreeRatio=99'

run_ant () {
  echo "#--- ENTERING $DIR/../$1" >> $LOG
  cd $DIR/../$1
  ant clean >> $LOG
  ant >> $LOG
}

build_db () {
  echo "#--- building $1 db"
  cd $DIR/../$2
  ant clean >> $LOG
  ant build-db >> $LOG
}

build () {
  echo "#--- building $1"
  run_ant "$2"
}

run_build () {
  echo '#--- building '$1
  run_ant "$1/main"
}

run_tests () {
  echo "#--- testing $1"
  run_ant "$1/test"
}

build_and_test () {
  run_build "$1"
  run_tests "$1"
}

# ANT_OPTS must be set for tests to run.
if test -z "$ANT_OPTS"; then
  ANT_OPTS=$DEFAULT_ANT_OPTS
fi

if test -z $PSQL_USER; then
    PSQL_USER=$USER
fi
if test -z $PSQL_PWD; then
    PSQL_PWD=$USER;
fi

for dep in psql createdb ant; do
  if test -z $(which $dep); then
    echo "ERROR: $dep not found - please make sure $dep is installed and configured correctly"
    exit 1
  fi
done

echo '#--- checking configuration'
for file in $PROP_FILE $BIO_FILE; do
  if test ! -f $IMDIR/$file; then
      echo "#--- $file not found. Providing default properties file."
      cp $DIR/$file $IMDIR/
      sed -i=bak -e "s/USERNAME/$PSQL_USER/g" $IMDIR/$file
      sed -i=bak -e "s/PASSWORD/$PSQL_PWD/g" $IMDIR/$file
      echo "#--- Created new properties file: $IMDIR/$file"
  fi
done

echo '#--- checking databases'
for db in $DBS; do 
    if psql --list | egrep -q '\s'$db'\s'; then
        echo '#--- '$db exists.
    else
        echo '#--- Creating '$db
        createdb $db
    fi
done

build_and_test intermine/model

run_build intermine/objectstore

build testmodel intermine/objectstore/model/testmodel

run_tests intermine/objectstore

run_build intermine/integrate

build 'fulldata model' intermine/integrate/model/fulldata

run_tests intermine/integrate

build_and_test intermine/pathquery

build_and_test intermine/api

build 'userprofile' intermine/api/model/userprofile

build_db 'bio model' bio/test-all/dbmodel

build_and_test bio/core
