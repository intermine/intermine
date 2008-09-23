#!/bin/bash
# 
# default usage: valmine.sh submission_name
#
# note: you should put the db password in ~/.pgpass if don't
#       want to be prompted for it
#
# TODO: deal with directory structure in ftp://dcc/pub: use wget -r ?  
#
#
# sc 09/08
#

REL=val

#getting some setting from the properties file
DBHOST=`grep metadata.datasource.serverName $HOME/modmine.properties.$REL | awk -F "=" '{print $2}'`
DBUSER=`grep metadata.datasource.user $HOME/modmine.properties.$REL | awk -F "=" '{print $2}'`
DBPW=`grep metadata.datasource.password $HOME/modmine.properties.$REL | awk -F "=" '{print $2}'`

FTPURL=ftp://ftp.modencode.org/pub/dcc
DATADIR=/shared/data/modmine/subs/chado
DBDIR=/shared/data/modmine/
CHADODB=modchado-$REL
MINEDB=modmine-$REL

MINEDIR=$HOME/svn/dev/modmine
SOURCES=modencode-static,entrez-organism,modencode-metadata

# these should not be edited
APPEND=n;
WEBAPP=y;
TESTS=y;
STAG=y;
V=;
F="exit 1";

progname=$0

function usage () {
   cat <<EOF

Usage: $progname [-a] [-s] [-t] [-w] [-v] [-f] submission_name
   -a: the submission will be APPENDED to the present validation mine
   -s: no new loading of chado (stag is not run)
   -t: no acceptance test run
   -w: no new webapp will be built
   -v: verbode mode
   -f: force building of mine after a failure in the loading in chado 
   
Notes: The file is downloaded only if not present or the remote copy 
      is newer
      
example
       $progname submission_name
  
EOF
   exit 0
}

while getopts ":astwvf" opt; do
   case $opt in

   a )  echo; echo "The submission will be added to the present validation mine." ; 
      APPEND=y;;
#   b )  echo "found -b and $OPTARG is after -b" ;;
   s )  echo; echo "Using previous load of chado (stag is not run)" ; STAG=n;;
   t )  echo; echo "No acceptance test run" ; TESTS=n;;
   w )  echo; echo "No new webapp will be built" ; WEBAPP=n;;
   f )  echo; echo "Forcing mode: will continue if loading in chado gives errors." ; F=;;
   v )  echo; echo "Verbose mode" ; V=-v;;
   h )  usage ;;
   \?)  usage ;;
   esac
done

shift $(($OPTIND - 1))

#---------------------------------------
# getting the chadoxml from ftp site
#---------------------------------------
cd $DATADIR

#...and get it if the remote timestamp is newer than the local one
#(or of a different size)
# we only try when we want to update chado... 

if [ $STAG = "y" ]
then

wget -N $FTPURL/$1.chadoxml

fi
#echo "press return to continue.."
#read 

#---------------------------------------
# building the chado db
#---------------------------------------
#
# note: it could fail if any connection active
#

if [ "$APPEND" = "n" ] && [ "$STAG" = "y" ]
then

dropdb -e $CHADODB -h $DBHOST -U $DBUSER

createdb -e $CHADODB -h $DBHOST -U $DBUSER\
|| { printf "%b" "\nMine building FAILED. Please check previous error message.\n\n" ; exit 1 ; }

psql -d $CHADODB -h $DBHOST -U $DBUSER < $DBDIR/build_empty_chado.sql\
|| { printf "%b" "\nMine building FAILED. Please check previous error message.\n\n" ; exit 1 ; }

fi

#---------------------------------------
# filling chado db
#---------------------------------------

if [ $STAG = "y" ]
then
echo 
echo "filling the chado db with $1..."
stag-storenode.pl -D "Pg:$CHADODB@$DBHOST" -user $DBUSER -password\
 $DBPW -noupdate cvterm,dbxref,db,cv,feature $1.chadoxml\
 || { printf "%b" "\n modMine FAILED.\n" ; $F ; }

#echo "press return to continue.."
#read 
else
echo
echo "Using previously loaded chado."
echo
fi

#---------------------------------------
# building modmine
#---------------------------------------
cd $MINEDIR

echo "Building modMine VAL"
echo

../bio/scripts/project_build -a $SOURCES -V $REL $V -b -t localhost /tmp/mod-meta\
 || { printf "%b" "\n modMine build FAILED.\n" ; exit 1 ; }

#echo "press return to continue.."
#read 


#---------------------------------------
# building webapp
#---------------------------------------

if [ "$WEBAPP" = "y" ]
then
cd $MINEDIR/webapp
ant -Drelease=$REL $V default remove-webapp release-webapp
fi

if [ "$TESTS" = "y" ]
then
#---------------------------------------
# and run acceptance tests
#---------------------------------------
echo
echo "running acceptance tests"
echo
cd $MINEDIR/integrate
ant $V -Drelease=$REL acceptance-tests

mv $MINEDIR/integrate/build/acceptance_test.html $MINEDIR/integrate/build/$1.html
#xterm -bg grey20 -hold -e "elinks file://$MINEDIR/integrate/build/$1.html" &
elinks $MINEDIR/integrate/build/$1.html

echo
echo "acceptance test results in "
echo "$MINEDIR/integrate/build/$1.html"
echo
#echo "press return to continue.."
#read 
fi

