#!/bin/bash
# 
# default usage: valmine.sh submission_name
#
# note: you should put the db password in ~/.pgpass if don't
#       want to be prompted for it
#
# sc 09/08
#

REL=val

DBHOST=bert
DBUSER=sc
DBPW=sc

FTPURL=ftp://ftp.modencode.org/pub/dcc
DATADIR=/shared/data/modmine/subs/chado
DBDIR=/shared/data/modmine/
CHADODB=modchado-$REL
MINEDB=modmine-$REL

MINEDIR=~/svn/dev/modmine
SOURCES=modencode-static,entrez-organism,modencode-metadata

# these should not be edited
WEBAPP=y;
APPEND=n;
V=;
F="exit 1";

progname=$0

function usage () {
   cat <<EOF

Usage: $progname [-a] [-n] [-v] [-f] submission_name
   -a: the submission will be APPENDED to the present validation mine
   -n: no new webapp will be built
   -v: verbode mode
   -f: force building of mine after a failure in the loading in chado 
   
Notes: The file is downloaded only if not present or the remote copy 
      is newer (in this case a copy of the older should be created)
      
       Without the -a option, the build will fail if there are active 
      connections to the chado database. 

example
       $progname submission_name
  
EOF
   exit 0
}

while getopts ":navf" opt; do
   case $opt in

   a )  echo; echo "The submission will be added to the present validation mine." ; 
      APPEND=y;;
#   b )  echo "found -b and $OPTARG is after -b" ;;
   n )  echo; echo "No new webapp will be built" ; WEBAPP=n;;
   f )  echo; echo "Forcing mode: will continue if loading in chado gives errors." ; F=;;
   v )  echo; echo "Verbose mode" ; V=-v;;
   h )  usage ;;
   \?)  usage ;;
   esac
done

shift $(($OPTIND - 1))
#echo "the remaining arguments are: $1 $2 $3"

#echo
#echo "press return to load $1.."
#read 

#---------------------------------------
# getting the chadoxml from ftp site
#---------------------------------------

cd $DATADIR

#...and get it if the remote timestamp is newer than the local
# it will make a copy of the local (as name.chadoxml.n)
wget -N $FTPURL/$1.chadoxml

echo "press return to continue.."
read 

#---------------------------------------
# building the chado db
#---------------------------------------
#
# NB: FAILS IF SOME CONNECTION ACTIVE
#

if [ "$APPEND" == "n" ]
then
dropdb -e $CHADODB -h $DBHOST -U $DBUSER 
createdb -e $CHADODB -h $DBHOST -U $DBUSER 

psql -d $CHADODB -h $DBHOST -U $DBUSER < $DBDIR/build_empty_chado.sql
#echo "press return to continue.."
#read 
fi

#---------------------------------------
# filling chado db
#---------------------------------------
echo 
echo "filling the chado db with $1..."
stag-storenode.pl -D "Pg:$CHADODB@$DBHOST" -user $DBUSER -password\
 $DBPW -noupdate  cvterm,dbxref,db,cv,feature $1.chadoxml \
 || { printf "%b" "\n stag-storenode FAILED.\n" ; $F ; }

#echo "press return to continue.."
#read 


#---------------------------------------
# building modmine
#---------------------------------------

cd $MINEDIR

../bio/scripts/project_build -a $SOURCES -V $REL $V -b -t localhost /tmp/mod-meta

#echo "press return to continue.."
#read 


#---------------------------------------
# building webapp
#---------------------------------------

if [ "$WEBAPP" == "y" ]
then
cd webapp
ant -Drelease=$REL $V default remove-webapp release-webapp
fi

