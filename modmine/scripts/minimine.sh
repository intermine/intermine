#!/bin/bash
#
# default usage: minimine.sh dccid
#         it will - create an appropriate properties file modmine.properties.dccid
#                 - build a modmine webapp for the data in the relevant schema in chado
#                 - run some acceptance tests
#                 - send a report to the feedback.destination in the properties file
#
# note: you should put the db password in ~/.pgpass if don't
#       want to be prompted for it
#
# sc
#
# see after argument parsing for all envs related to the release
#
# ASSUMPTIONS:
#     - to be run from a checkout, in the modmine directory
#     - a modmine.properties file is present in your $HOME/.intermine directory
#       a template of the file is copied at the end of this file
#       please check and adapt it (particularly chado db and tomcat setting).
#     - modchado dbuser is different from the other modmine dbs
#     - we assume shemas named like u'dccid', e.g. u220
#
# here our sql setting and preliminary steps
#
# createuser -h modprod1 -U modmine -s -P modvet
# createuser -h modprod1 -U modmine -s -P chadovet
#
# createdb -E SQL_ASCII common-tgt-items-modmine-vet -h modprod1 -U modvet
# createdb modmine-vet-userprofile -h modprod1 -U modvet
# and in modmine/webapp we run
# ant -v build-db-userprofile
#


REPDIR=$HOME/reports
#RECIPIENTS=
PROPDIR=$HOME/.intermine
SCRIPTDIR=../flymine-private/scripts/modmine/
REPORTPATH=file://$REPDIR
SOURCES=modmine-static,modencode-metadata
#SOURCES=modmine-static,modencode-metadata,entrez-organism

# TODO add check that modmine in path
MINEDIR=$PWD
BUILDDIR=$MINEDIR/integrate/build

# these should not be edited
WEBAPP=y       #defaults: build a webapp
V=             #          non-verbose mode
TEST=y         #          do acceptance tests
TIMESTAMP=`date "+%y%m%d.%H%M"`  # used in the log
INTERACT=n     #          y: step by step interaction
REL=dccid      # generic

progname=$0

function usage () {
  cat <<EOF

Usage: $progname  [-i] [-t] [-w] [-v]]
  -i: interactive mode (for testing)
  -t: no acceptance test run
  -w: no webapp will be built
  -v: verbode mode

examples:

$progname	37
                    - build vetting mine for dccid 37 querying chado db in schema 'u37'
                    - run acceptance tests
                    - build the webapp

$progname	-v -w	37
                    - build vetting mine for dccid 37 querying chado db in schema 'u37'
                    - verbose mode
                    - run acceptance tests
                    - don't build the webapp
EOF
  exit 0
}

while getopts ":Vitvw" opt; do
  case $opt in

  i )  echo; echo "Interactive mode" ; INTERACT=y;;
  t )  echo; echo "No acceptance test run" ; TEST=n;;
  v )  echo; echo "Verbose mode" ; V=-v;;
  w )  echo; echo "Webapp will not be built" ; WEBAPP=n;;
  h )  usage ;;
  \?)  usage ;;
  esac
done

shift $(($OPTIND - 1))

# set release (default dev)
 if [ -n "$1" ]
 then
     REL=$1
else
echo
echo "You need to specify which submission you want to process (dcc id)"
echo
exit;
 fi


echo $REL

#
# creating the new properties file
#

sed "s/DCCID/$REL/g" $PROPDIR/modmine.properties > $PROPDIR/modmine.properties.$REL

#
# Getting some values from the properties file.
# NOTE: it is assumed that dbhost is the same for chado and modmine
#     -a to grep also (alleged) binary files
#     a separate dbuser is employed in chado
#

DBHOST=`grep -a metadata.datasource.serverName $PROPDIR/modmine.properties.$REL | awk -F "=" '{print $2}'`
CHADOUSER=`grep -a metadata.datasource.user $PROPDIR/modmine.properties.$REL | awk -F "=" '{print $2}'`
DBPW=`grep -a metadata.datasource.password $PROPDIR/modmine.properties.$REL | awk -F "=" '{print $2}'`
CHADODB=`grep -a metadata.datasource.databaseName $PROPDIR/modmine.properties.$REL | awk -F "=" '{print $2}'`
MINEDB=`grep -a db.production.datasource.databaseName $PROPDIR/modmine.properties.$REL | awk -F "=" '{print $2}'`
BASEURL=`grep -a webapp.deploy.url $PROPDIR/modmine.properties.$REL | awk -F "=" '{print $2}'`

MINEURL=$BASEURL/modmine$REL
RECIPIENTS=`grep -a feedback.destination $PROPDIR/modmine.properties.$REL | awk -F "=" '{print $2}'`

ERRLOG="$DATADIR/$REL-$TIMESTAMP"     # general error log

echo
echo "================================="
echo "Building modmine-$REL on $DBHOST."
echo "================================="
echo "Logs: $ERRLOG"
echo
echo "current directory: $MINEDIR"
echo

if [ $INTERACT = "y" ]
then
echo
echo "Press return to continue.."
echo -n "->"
read
fi

#---------------------------------------
# build modmine
#---------------------------------------
cd $MINEDIR

echo "Building modMine $REL"
echo

# create new mine, set search path in chado and fill new mine
createdb -E SQL_ASCII modmine-$REL -h modprod1 -U modvet
psql -H -h $DBHOST -d $CHADODB -U $CHADOUSER -c 'alter user '$CHADOUSER' set search_path = u'$REL',public;'
../bio/scripts/project_build -a $SOURCES -V $REL $V -b -t localhost /tmp/mod-meta\
|| { printf "%b" "\n modMine build FAILED.\n" ; exit 1 ; }

if [ $INTERACT = "y" ]
then
echo
echo "press return to continue.."
read
fi

#---------------------------------------
# and run acceptance tests
#---------------------------------------

if [ "$TEST" = "y" ]
then
echo
echo "========================"
echo "running acceptance tests"
echo "========================"
echo
cd $MINEDIR/integrate

ant $V -Drelease=$REL acceptance-tests-metadata

# check chado for new features
cd $MINEDIR
$SCRIPTDIR/add_chado_feats_to_report.pl $DBHOST $CHADODB $CHADOUSER $BUILDDIR/acceptance_test.html > $REPDIR/$REL.html

echo "sending mail!!"
mail $RECIPIENTS -s "$REL report, also in $REPORTPATH/$REL.html" < $REPDIR/$REL.html

#elinks $REPORTS/$REL.html

echo
echo "acceptance test results in "
echo "$REPORTPATH/$REL.html"
echo
fi

#---------------------------------------
# building webapp
#---------------------------------------
if [ "$WEBAPP" = "y" ]
then
cd $MINEDIR/webapp
ant -Drelease=$REL $V default remove-webapp release-webapp
echo
echo "Vetting modMine for $REL deployed at "
echo "$MINEURL"
echo
fi


# ==================================
# PROPERTIES FILE modmine.properties
# ==================================

# os.query.max-time=90000000
# os.query.max-limit=9500000
# os.query.max-offset=9500000
# os.queue-len=100
#
# os.production.logfile=productionLog
#
# os.production.class=org.intermine.objectstore.intermine.ObjectStoreInterMineImpl
# os.production.alias=modmine
# os.production.modmine.db=db.production
# os.production.model=genomic
# os.production.modmine.truncatedClasses=org.flymine.model.genomic.BioProperty
#
# db.production.datasource.class=org.postgresql.jdbc3.Jdbc3PoolingDataSource
# db.production.datasource.dataSourceName=db.production
# db.production.datasource.maxConnections=50
# db.production.driver=org.postgresql.Driver
# db.production.platform=PostgreSQL
#
# os.production.verboseQueryLog=true
# os.production.minBagTableSize=3000
#
#
# #
# # COMMON TARGET ITEMS DATABASE
# #
# db.common-tgt-items.datasource.serverName=modprod1
# db.common-tgt-items.datasource.databaseName=common-tgt-items-modmine-vet
# db.common-tgt-items.datasource.user=modvet
# db.common-tgt-items.datasource.password=modvet
#
# #
# # DCC METADATA
# #
# db.modencode-metadata.datasource.serverName=modprod1
# db.modencode-metadata.datasource.databaseName=modchado
# db.modencode-metadata.datasource.user=chadovet
# db.modencode-metadata.datasource.password=chadovet
#
# #
# # PRODUCTION DB
# #
# db.production.datasource.serverName=modprod1
# db.production.datasource.databaseName=modmine-DCCID
# db.production.datasource.user=modvet
# db.production.datasource.password=modvet
#
# #
# # USERPROFILE
# #
# db.userprofile-production.datasource.serverName=modprod1
# db.userprofile-production.datasource.databaseName=modmine-vet-userprofile
# db.userprofile-production.datasource.user=modvet
# db.userprofile-production.datasource.password=modvet
#
# #build.compiler=jikes
# build.compiler=modern
#
# # Web server
# #================
# www.serverlocation=modmine@mod2:/var/www/html
#
# # Web application
# #================
# webapp.deploy.url=http://mod2.modencode.org:8080
# webapp.baseurl=http://mod2.modencode.org:8080
# webapp.path=modmineDCCID
# webapp.logdir=/webapp/apache-tomcat-5.5.25/logs
#
# # tomcat manager
# webapp.manager=manager
# webapp.password=password
#
# # ObjectStores to use - this should not need to change
# webapp.os.alias=os.production
# webapp.userprofile.os.alias=osw.userprofile-production
# webapp.viewByID.prefix=report.do?id=
#
# # gbrowse properties
# #===================
# gbrowse.prefix=http://modencode.oicr.on.ca/cgi-bin/gbrowse
# gbrowse_image.prefix=http://modencode.oicr.on.ca/cgi-bin/gbrowse_img
#
# mail.host=mail.flymine.org
# mail.from=help@modencode.org
# mail.subject=Password for modMine
# mail.text=Your password is: {0}
#
#
# # project settings
# #=================
# project.title=modMine DCCID
# project.subTitle=A test data warehouse for submission DCCID
# project.sitePrefix=http://intermine.modencode.org
# project.helpLocation=http://intermine.modencode.org/help
# project.releaseVersion=DCCID
# project.contact=<a href="mailto:sergio%5Bat%5Dmodmine.org">sergio[at]modmine.org</a>
# project.funded.by=modENCODE is funded by the <a href="http://www.genome.gov/25521166/">NIH</a>
#
# superuser.account=kmr@flymine.org
#
# portal.welcome = Welcome to vetting modMine
#
# feedback.destination = sergio@modencode.org
#
# project.standalone=true



