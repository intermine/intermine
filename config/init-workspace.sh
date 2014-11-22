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

build () {
  echo '#--- building '$1
  cd $DIR/../$2
  ant clean >> $LOG
  ant >> $LOG
}

build_db () {
  build $1 $2
  ant build-db >> $LOG
}

# ANT_OPTS must be set for tests to run.
if test -z $ANT_OPTS; then
  ANT_OPTS='-server -XX:MaxPermSize=256M -Xmx1700m -XX:+UseParallelGC -Xms1700m -XX:SoftRefLRUPolicyMSPerMB=1 -XX:MaxHeapFreeRatio=99'
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

build model intermine/model/main

build "model tests" intermine/model/test

build objectstore intermine/objectstore/main

build testmodel intermine/objectstore/model/testmodel

build 'objectstore tests' intermine/objectstore/test

build integrate intermine/integrate/main

build 'fulldata model' intermine/integrate/model/fulldata

build 'integrate tests' intermine/integrate/test

build pathquery intermine/pathquery/main

build api intermine/api/main

build 'api tests' intermine/api/test

build 'userprofile' intermine/api/model/userprofile

build_db 'bio model' bio/test-all/dbmodel

build 'bio core' bio/core/main

build 'core tests' bio/core/test
