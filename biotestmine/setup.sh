set -e

DIR="$(cd $(dirname "$0"); pwd)"
MINENAME=biotestmine
PROD_DB=$MINENAME
ITEMS_DB=$MINENAME-items
USERPROFILE_DB=$MINENAME-userprofile
IMDIR=$HOME/.intermine
PROP_FILE=${MINENAME}.properties
DATA_DIR=$HOME/${MINENAME}-sample-data

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

# Report settings before we do anything.
if test $DEBUG; then
    echo SETTINGS:
    echo " DIR = $DIR"
    echo " MINENAME = $MINENAME"
    echo " PROD_DB = $PROD_DB"
    echo " ITEMS_DB = $ITEMS_DB"
    echo " USERPROFILE_DB = $USERPROFILE_DB"
    echo " IMDIR = $IMDIR"
    echo " PROP_FILE = $PROP_FILE"
    echo " DATA_DIR = $DATA_DIR"
    echo " SERVER = $SERVER"
    echo " PORT = $PORT"
    echo " PSQL_USER = $PSQL_USER"
    echo " PSQL_PWD = $PSQL_PWD"
    echo " TOMCAT_USER = $TOMCAT_USER"
    echo " TOMCAT_PWD = $TOMCAT_PWD"
fi

if test ! -d $DIR/log; then
    mkdir $DIR/log
fi

if test ! -d $IMDIR; then
    echo Making .intermine configuration directory.
    mkdir $IMDIR
fi

if test ! -f $IMDIR/$PROP_FILE; then
    echo $PROP_FILE not found. Providing default properties file...
    cd $IMDIR
    cp $DIR/../bio/tutorial/malariamine.properties $PROP_FILE
    sed -i "s/PSQL_USER/$PSQL_USER/g" $PROP_FILE
    sed -i "s/PSQL_PWD/$PSQL_PWD/g" $PROP_FILE
    sed -i "s/TOMCAT_USER/$TOMCAT_USER/g" $PROP_FILE
    sed -i "s/TOMCAT_PWD/$TOMCAT_PWD/g" $PROP_FILE
    sed -i "s/items-malariamine/$ITEMS_DB/g" $PROP_FILE
    sed -i "s/userprofile-malariamine/$USERPROFILE_DB/g" $PROP_FILE
    sed -i "s/databaseName=malariamine/databaseName=$PROD_DB/g" $PROP_FILE
    sed -i "s/malariamine/$MINENAME/gi" $PROP_FILE
    sed -i "s/localhost/$SERVER/g" $PROP_FILE
    sed -i "s/8080/$PORT/g" $PROP_FILE
    echo Created $PROP_FILE
fi

echo Checking databases...
for db in $USERPROFILE_DB $PROD_DB $ITEMS_DB; do
    if psql --list | egrep -q '\s'$db'\s'; then
        echo $db exists.
    else
        echo Creating $db ...
        createdb $db
    fi
done

if test -d $HOME/${MINENAME}-sample-data; then
    echo Sample data already exists.
else
    cd $HOME
    mkdir $DATA_DIR
    cd $DATA_DIR
    cp $DIR/../bio/tutorial/malaria-data.tar.gz .
    echo Unpacking sample data...
    tar -zxvf malaria-data.tar.gz >> $DIR/log/extract.log
    rm malaria-data.tar.gz
fi

cd $DIR
echo Personalising project.xml
sed -i "s!DATA_DIR!$DATA_DIR!g" project.xml
sed -i "s/malariamine/$MINENAME/g" project.xml

echo Adjusting priorities.
PRIORITIES=$DIR/dbmodel/resources/genomic_priorities.properties
echo 'ProteinDomain.shortName = interpro, uniprot-malaria' >> $PRIORITIES

cd $DIR/dbmodel
echo Building DB
ant clean build-db >> $DIR/log/build-db.log

echo 'Loading data (this could take some time) ...'
cd $DIR
../bio/scripts/project_build -b -v $SERVER $HOME/${MINENAME}-dump

cd $DIR/webapp
echo 'Building userprofile..'
ant build-db-userprofile >> $DIR/log/build-userprofile-db.log
echo 'Building web-application'
ant default >> $DIR/log/build-webapp.log
echo 'Releasing web-application'
ant remove-webapp release-webapp >> $DIR/log/build-webapp.log

echo All done. Logs available in $DIR/log

