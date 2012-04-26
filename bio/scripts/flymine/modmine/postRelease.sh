#!/bin/bash
#
# default usage: postRelease.sh
#
# sc

ARKDIR=/micklem/releases/modmine
DBHOST=modfast
DBUSER=modmine
DATADIR=/micklem/data/modmine/subs/chado
RELDIR="$DATADIR/ark"
LOADDIR="$DATADIR/load"
INTERACT=y
DOIT=y

#PRO="lieb henikoff macalpine oliver snyder karpen white celnikerlai waterstonpiano"
PRO="henikoff karpen waterstonpiano"

progname=$0

umask 0002

function usage () {
	cat <<EOF

Usage:
$progname [-b] 
	-b: batch mode (all operations are executed without warning)

examples:

$progname
$progname 33    deal with post release 33
$progname -b 33 idem, batch mode (careful!)

EOF
	exit 0
}

echo

while getopts ":b" opt; do
	case $opt in
	b )  echo "- BATCH mode" ; INTERACT=n;;
	h )  usage ;;
	\?)  usage ;;
	esac
done

shift $(($OPTIND - 1))


if [ -z "$1" ]
then
echo
echo -n "Please specify the NEW modMine release number: -> "
read REL
else
REL="$1"
fi

if [ ! -s "$ARKDIR/r$REL" ]
then
echo
echo "Creating directory $ARKDIR/r$REL..."
mkdir $ARKDIR/r$REL
mkdir $ARKDIR/r$REL/chado
mkdir $ARKDIR/r$REL/xml
fi

# set previous release
let PREL=$REL-1
echo
echo "--------------------------------------------------------------"
echo "Post release house keeping: dealing with releases $REL and $PREL"
echo "The projects considered are:"
echo "$PRO"
echo "--------------------------------------------------------------"
echo

function check_user {
USER=`whoami`

if [ "$USER" != "modmine" ]
then
echo
echo "IMPORTANT: you are not running this script as modmine: "
echo "You need to become modmine to release the archived webapp."
echo
#exit
fi
}

function prepare_production {
#mv dump to archive
echo
# use the build dump (from modfast)
mv $ARKDIR/build/mod-final.dmp.final $ARKDIR/r$REL/modmine-r$REL

#alt
#echo "Dumping current release $REL ..."
#pg_dump -F c -i -h modprod0 -f $ARKDIR/r$REL/modmine-r$REL modmine-r$REL -U modmine
#echo "Dumping modmine-build in modfast..."
#pg_dump -F c -i -h modfast -f $ARKDIR/r$REL/modmine-r$REL modmine-build -U modminebuild

#create release on production server
echo
echo "Creating new production modmine-r$REL on modprod0..."
createdb -E SQL_ASCII -h modprod0 -U modmine modmine-r$REL
echo
echo "Restoring build modmine-r$REL on modprod0..."
pg_restore -h modprod0 -U modmine -d modmine-r$REL $ARKDIR/r$REL/modmine-r$REL

echo "done"
echo
}


function dump_chadoes {
echo "========================================================"
echo "       DUMPING modchado INTO ARCHIVE DIRECTORY "
echo "       $ARKDIR/r$REL/chado"
echo "$PRO"
echo "========================================================"

for p in $PRO
do
echo -n "Dumping chado for $p..-------------------"
date "+%d%b%Y %H:%M"
pg_dump -F c -i -T wiggle_data -h $DBHOST -U $DBUSER -f $ARKDIR/r$REL/chado/modchado-$p modchado-$p

echo
echo -n "$p done: "
date "+%d%b%Y %H:%M"
echo
done

echo "bye!"
echo
}

function tar_xml {
RETURNDIR=$PWD
cd $LOADDIR

for p in $PRO
do
echo "Arkiving $p..-------------------"

sed 's/$/.chadoxml/g' $DATADIR/$p.live > $p.ark
# tar in bzip format using list in .ark
tar -c -v -j -T $p.ark -f $ARKDIR/r$REL/xml/$p.bz2 

#more $p.ark
#echo "now?"
#read
echo
echo "$p done"
echo
#rm $p.ark
done

cd $RETURNDIR
echo "All xml archived."
echo
}

function archive_mine {
#dump release: this is obsolete now
#echo "Dumping current release $REL ..."
#pg_dump -F c -i -h modprod0 -f $ARKDIR/r$REL/modmine-r$REL modmine-r$REL -U modmine
# use the build dump (from modfast)
# mv $ARKDIR/build/mod-final.dmp $ARKDIR/r$REL/modmine-r$REL
#echo "Dumping modmine-build in modfast..."
#pg_dump -F c -i -h modfast -f $ARKDIR/r$REL/modmine-r$REL modmine-build -U modminebuild

#create release on archive server
echo
echo "Creating empty archive modmine-r$REL on modalone..."
createdb -h modalone -U modmine  modmine-r$REL
echo
echo "Restoring archive modmine-r$REL on modalone..."
pg_restore -h modalone -U modmine -d modmine-r$REL $ARKDIR/r$REL/modmine-r$REL

# save war file (?, from modprod0)
#rcp -p contrino@modprod0:/webapp/apache-tomcat-6.0.18/webapps/release-$REL.war $ARKDIR/r$REL
#cp /webapp/apache-tomcat-6.0.18/webapps/release-$REL.war $ARKDIR/r$REL
echo "done"
echo
}

function do_branch {
# do branch 
RETURNDIR=$PWD
svn copy svn://svn.flymine.org/flymine/trunk svn://svn.flymine.org/flymine/branches/modmine/modmine-$REL -m "modmine $REL branch"
cd /data/code/modmine/
svn co svn://svn.flymine.org/flymine/branches/modmine/modmine-$REL modmine-$REL
cd $RETURNDIR
echo "done"
echo
}

function start_archive_webapp {
RETURNDIR=$PWD
echo
echo "Dumping $PREL userprofile..."
pg_dump -c -i -h modprod0 -U modmine -f $ARKDIR/userprofiles/modmine-r$PREL-userprofile modmine-prod-userprofile

echo; echo "Creating userprofile on modalone.."
createdb -h modalone -U modmine  modmine-r$PREL-userprofile

echo; echo "Restoring r$PREL userprofile on modalone..."
psql -h modalone -U modmine -d modmine-r$PREL-userprofile -f $ARKDIR/userprofiles/modmine-r$PREL-userprofile

echo; echo "Compressing archived dump..."
gzip $ARKDIR/userprofiles/modmine-r$PREL-userprofile

# needed for a more up to date list compared with db
echo; echo "Archiving osbag table for r$PREL..."
pg_dump -c -h modprod0 -U modmine -t osbag_int modmine-r$PREL > $ARKDIR/userprofiles/osbag_int-r$PREL.sql
gzip $ARKDIR/userprofiles/osbag_int-r$PREL.sql

echo; echo "Creating properties file for archived webapp r$PREL..."
cd /home/modmine/.intermine
#sed 's/modprod0/modalone/g' modmine.properties.r$PREL | grep -v 'google.analytics' > modmine.properties.modmine-$PREL
sed 's/modprod0/modalone/g' modmine.properties.r$PREL | sed 's/modmine-prod-userprofile/modmine-r'"$PREL"'-userprofile/g' | grep -v 'google.analytics' > modmine.properties.modmine-$PREL

echo; echo "Starting archived webapp r$PREL..."
cd /data/code/modmine/modmine-$PREL/modmine/webapp
ant -Drelease=modmine-$PREL default remove-webapp release-webapp

echo
echo "IMPORTANT: you are required to manually change:"
#echo "        /etc/httpd/conf.d/proxy_ajp.conf"
echo "        /etc/apache2/sites-available/intermine.modencode.org.conf"
#echo "on mod2 and restart the server with"
echo "on web0 and restart the server with"
echo "        sudo /usr/sbin/apachectl restart"
echo 

cd $RETURNDIR
echo "done"
echo
}

function do_modlog {
RETURNDIR=$PWD
echo

if [ ! -s "$RELDIR/$REL" ]
then
echo
echo "Creating directory $RELDIR/$REL..."
mkdir $RELDIR/$REL
echo "... and copying the .live files"
cp $DATADIR/*.live  $RELDIR/$REL
cp $DATADIR/deprecation.table $RELDIR/$REL
cp $DATADIR/deprecations $RELDIR/$REL
cp $DATADIR/superseded.table $RELDIR/$REL
mkdir $RELDIR/$REL/delta
fi

DELTA=$RELDIR/$REL/delta

echo
echo "--------------------------------------------------------------"
echo "Building modification log between releases $REL and $PREL"
echo "The projects considered are:"
echo "$PRO"
echo "--------------------------------------------------------------"
echo

LOG="$DATADIR/$REL"_changes.log
echo "modMine $REL changelog" > $LOG
echo "=========================================" >>$LOG


cd $DATADIR

for p in $PRO
do
echo "$p.."
echo "$p project" >> $LOG
echo >> $LOG
grep -vwf $RELDIR/$PREL/$p.live $p.live > $DELTA/$p.delta

grep -wf $DELTA/$p.delta deprecation.table | sed 's/->/deprecated by/g' >> $LOG
grep -wf $DELTA/$p.delta superseded.table | sed 's/->/superseded by/g' >> $LOG
grep -vwf $LOG $DELTA/$p.delta |  sed '1i\---- new ----'>> $LOG

echo "=========================================" >>$LOG
done

cd $RETURNDIR
echo "done"
echo   
}



function interact {
if [ "$INTERACT" = "y" ]
then
echo "** $1"
echo "Press return to continue or [n] to skip this step (^C to exit).."
echo -n "->"
read DOIT
fi

}


########################################
#
# MAIN
#
########################################

check_user

interact "Preparing production database modmine-r$REL on modprod0"
if [ "$DOIT" != "n" ]
then
prepare_production
fi

interact "Archiving new mine modmine-r$REL:"
if [ "$DOIT" != "n" ]
then
archive_mine
fi

interact "Branching in svn for release $REL:"
if [ "$DOIT" != "n" ]
then
do_branch
fi

interact "Start archived webapp modmine $PREL:"
if [ "$DOIT" != "n" ]
then
start_archive_webapp
fi

interact "Build a log of the modifications in the $REL release vs $PREL:"
if [ "$DOIT" != "n" ]
then
do_modlog
fi

interact "Archiving xml files for all the projects:"
if [ "$DOIT" != "n" ]
then
tar_xml
fi

interact "Dumping chadoes for all the projects:"
if [ "$DOIT" != "n" ]
then
dump_chadoes
fi

