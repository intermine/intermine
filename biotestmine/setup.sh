#!/bin/bash

# Build and release a biological test mine.

set -e
set -o pipefail # Pipes are considered failed if any of their commands fail.

DIR="$(cd $(dirname "$0"); pwd)"
MINENAME=biotestmine
PROD_DB=$MINENAME
ITEMS_DB=$MINENAME-items
USERPROFILE_DB=$MINENAME-userprofile
IMDIR=$HOME/.intermine
PROP_FILE=${MINENAME}.properties
DATA_DIR=$HOME/${MINENAME}-sample-data
LOG_DIR=$DIR/log
LOAD_LOG="${LOG_DIR}/load-data.log"
PROJECT_BUILD="${DIR}/../bio/scripts/project_build"
PRIORITIES=$DIR/dbmodel/resources/genomic_priorities.properties

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
if test -z $DB_ENCODING; then
    DB_ENCODING=SQL_ASCII
fi

for dep in perl psql createdb ant; do
  if test -z $(which $dep); then
    echo "ERROR: $dep not found - please make sure $dep is installed and configured correctly"
    exit 1
  fi
done

perl -MXML::Parser::PerlSAX \
     -MText::Glob \
     -MCwd \
     -MGetopt::Std \
     -e 'print "#--- Perl dependencies satisfied\n";'

# Report settings before we do anything.
if test $DEBUG; then
    echo '# SETTINGS:'
    echo "#  DIR = $DIR"
    echo "#  MINENAME = $MINENAME"
    echo "#  PROD_DB = $PROD_DB"
    echo "#  ITEMS_DB = $ITEMS_DB"
    echo "#  USERPROFILE_DB = $USERPROFILE_DB"
    echo "#  IMDIR = $IMDIR"
    echo "#  PROP_FILE = $PROP_FILE"
    echo "#  DATA_DIR = $DATA_DIR"
    echo "#  SERVER = $SERVER"
    echo "#  PORT = $PORT"
    echo "#  PSQL_USER = $PSQL_USER"
    echo "#  PSQL_PWD = $PSQL_PWD"
    echo "#  TOMCAT_USER = $TOMCAT_USER"
    echo "#  TOMCAT_PWD = $TOMCAT_PWD"
    echo "#  DB_ENCODING = $DB_ENCODING"
fi

if test ! -d $LOG_DIR; then
    mkdir $LOG_DIR
fi

if test ! -d $IMDIR; then
    echo '#---> Making .intermine configuration directory.'
    mkdir $IMDIR
fi

if test ! -f $IMDIR/$PROP_FILE; then
    echo "#---> $PROP_FILE not found. Providing default properties file..."
    cd $IMDIR
    cp $DIR/../bio/tutorial/malariamine.properties $PROP_FILE
    sed -i=bak "s/PSQL_USER/$PSQL_USER/g" $PROP_FILE
    sed -i=bak "s/PSQL_PWD/$PSQL_PWD/g" $PROP_FILE
    sed -i=bak "s/TOMCAT_USER/$TOMCAT_USER/g" $PROP_FILE
    sed -i=bak "s/TOMCAT_PWD/$TOMCAT_PWD/g" $PROP_FILE
    sed -i=bak "s/items-malariamine/$ITEMS_DB/g" $PROP_FILE
    sed -i=bak "s/userprofile-malariamine/$USERPROFILE_DB/g" $PROP_FILE
    sed -i=bak "s/databaseName=malariamine/databaseName=$PROD_DB/g" $PROP_FILE
    sed -i=bak "s/malariamine/$MINENAME/gi" $PROP_FILE
    sed -i=bak "s/localhost/$SERVER/g" $PROP_FILE
    sed -i=bak "s/8080/$PORT/g" $PROP_FILE
    echo "#--- Created $PROP_FILE"
fi

echo '#---> Checking databases...'
for db in $USERPROFILE_DB $PROD_DB $ITEMS_DB; do
    if psql --list | egrep -q '\s'$db'\s'; then
        echo "#--- $db exists."
    else
        echo "#---> Creating $db with encoding $DB_ENCODING ..."
        createdb --template template0 \
                 --username $PSQL_USER \
                 --encoding $DB_ENCODING \
                 $db
    fi
done

if test -d $HOME/${MINENAME}-sample-data; then
    echo '#--- Sample data already exists.'
else
    cd $HOME
    mkdir $DATA_DIR
    cd $DATA_DIR
    cp $DIR/../bio/tutorial/malaria-data.tar.gz .
    echo '#---> Unpacking sample data...'
    tar -zxvf malaria-data.tar.gz >> $DIR/log/extract.log
    rm malaria-data.tar.gz
fi

cd $DIR
if test ! -f project.xml; then
    echo '#---> Copying over malariamine project.xml'
    cp ../bio/tutorial/project.xml .
fi

echo '#---> Personalising project.xml'
sed -i=bak "s!DATA_DIR!$DATA_DIR!g" project.xml
sed -i=bak "s/malariamine/$MINENAME/g" project.xml

if egrep -q ProteinDomain.shortName $PRIORITIES; then
    echo '#--- Integration key exists.'
else
    echo '#---> Adjusting priorities.'
    echo 'ProteinDomain.shortName = interpro, uniprot-malaria' >> $PRIORITIES
fi

cd $DIR/dbmodel
echo '#---> Building DB'
ant clean build-db >> $DIR/log/build-db.log

if test ! -f $PROJECT_BUILD; then
    echo "ERROR: Cannot find project build script at $PROJECT_BUILD"
    exit 1
fi

echo '#---> Loading data (this could take some time) ...'
cd $DIR
$PROJECT_BUILD -b -v $SERVER $HOME/${MINENAME}-dump 2>&1 \
    | tee -a $LOAD_LOG \
    | grep -E '(action.*took|failed)'

echo '#--- Finished loading data.'
cp pbuild.log $DIR/log/

cd $DIR/webapp
echo '#---> Building userprofile..'
ant build-db-userprofile >> $DIR/log/build-userprofile-db.log
echo '#---> Building web-application'
ant default >> $DIR/log/build-webapp.log
echo '#---> Releasing web-application'
ant remove-webapp release-webapp | tee -a $DIR/log/build-webapp.log | grep tomcat-deploy

echo BUILD COMPLETE: Logs available in $DIR/log

