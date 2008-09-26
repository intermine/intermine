#!/bin/bash
# 
# default usage: domodmine.sh rel
#
# note: you should put the db password in ~/.pgpass if don't
#       want to be prompted for it
#
# sc 09/08
#
# TODO: use wget -r ?
#       better error logging 
#

# see after argument parsing for all envs related to the release

FTPURL=ftp://ftp.modencode.org/pub/dcc
DATADIR=/shared/data/modmine/subs/chado
DBDIR=/shared/data/modmine/
DATELOG=loading_times.log

MINEDIR=$HOME/svn/dev/modmine
SOURCES=modencode-static,entrez-organism,modencode-metadata

# these should not be edited
WEBAPP=y;   #defaults: build a webapp
APPEND=n;   #          rebuild the db
BUP=y;      #          do a back up copy of the modchado database       
V=;         #          non-verbose mode
F=;         #          continue stag loading (also after errors)
REL=dev;    #          if no release is passed, do a dev
DIR=        #          if files in a subdirectory
ONLYMETA=y  #          do only metadata

progname=$0

function usage () {
   cat <<EOF

Usage: $progname [-a] [-b] [-c] [-w] [-d directory_name] [-v] 
   -a: the submission will be APPENDED to the present mine
   -b: no back-up of modchado-$REL will be built
   -c: all data, not only meta-data (FB and WB)
   -w: no new webapp will be built
   -d: you can specify a subdirectory in $FTPURL where to look for files
   -v: verbode mode
   
 Note: The file is downloaded only if not present or the remote copy 
      is newer or has a different size. 
      
examples:

$progname
         will build a modmine-dev with metadata only
$progname -c test
         will build a modmine-test with metadata, Flybase and Wormbase
$progname -a -d ready2publish dev
         will add to modmine-dev all the new submissions in $FTPURL/ready2publish
$progname -a test
         will add to modmine-test all the new submissions in $FTPURL

EOF
   exit 0
}

while getopts ":abcwd:v" opt; do
   case $opt in

   a )  echo; echo "Append to exinting mine." ; APPEND=y;;
   b )  echo; echo "Don't build a back-up of the database." ; BUP=n;;
   c )  echo; echo "Do all (not only meta-data)." ; ONLYMETA=n;;
   d )  echo "processing $FTPURL/$OPTARG directory" ; DIR=/$OPTARG;;
   w )  echo; echo "No new webapp will be built" ; WEBAPP=n;;
#   f )  echo; echo "Forcing mode: will continue if loading in chado gives errors." ; F=;;
   v )  echo; echo "Verbose mode" ; V=-v;;
   h )  usage ;;
   \?)  usage ;;
   esac
done

shift $(($OPTIND - 1))

if [ -n "$1" ]
then
REL=$1
fi
CHADODB=modchado-$REL
MINEDB=modmine-$REL
DATELOG=loading_times.$REL.log

DBHOST=`grep metadata.datasource.serverName $HOME/modmine.properties.$REL | awk -F "=" '{print $2}'`
DBUSER=`grep metadata.datasource.user $HOME/modmine.properties.$REL | awk -F "=" '{print $2}'`
DBPW=`grep metadata.datasource.password $HOME/modmine.properties.$REL | awk -F "=" '{print $2}'`

# this is to avoid to grep on a non-existent file
touch $DATELOG

echo
echo "building modmine-$REL on $DBHOST.."
read 

#---------------------------------------
# getting the chadoxml from ftp site
#---------------------------------------

cd $DATADIR

#...and get it if the remote timestamp is newer than the local
# it will make a copy of the local (as name.chadoxml.n)
# NB: this assume that the ftp 
wget -N $FTPURL$DIR/*.chadoxml
#wget -r -nd -np -l 2 -N $FTPURL/*chadoxml  ?? to test

#echo "press return to continue.."
#read 

#---------------------------------------
# building the chado db
#---------------------------------------
#

if [ "$BUP" = "y" ]
then
createdb -e "$CHADODB"-old -T $CHADODB -h $DBHOST -U $DBUSER\
|| { printf "%b" "\nMine building FAILED. Please check previous error message.\n\n" ; exit 1 ; }
fi

if [ "$APPEND" = "n" ]
then
dropdb -e $CHADODB -h $DBHOST -U $DBUSER;
createdb -e $CHADODB -h $DBHOST -U $DBUSER || { printf "%b" "\nMine building FAILED. Please check previous error message.\n\n" ; exit 1 ; }

#echo "press return to continue.."
#read 

psql -d $CHADODB -h $DBHOST -U $DBUSER < $DBDIR/build_empty_chado.sql\
|| { printf "%b" "\nMine building FAILED. Please check previous error message.\n\n" ; exit 1 ; }
#echo "press return to continue.."
#read 
fi

#---------------------------------------
# filling chado db
#---------------------------------------
echo 
echo "filling chado db ..."

for sub in *.chadoxml
do
# check previous loading status. 
# it is useful also when not doing an 'append': in fact you can edit
# the loading_times.$REL.log file in the datadir and remove from a complete list
# the files you want to reload.
#

#if [ $APPEND = "y" ] && [ $sub == "`grep $sub $DATELOG`" ]
if [ $sub == "`grep $sub $DATELOG`" ]
then
 echo "$sub already in!!"
 continue
fi
#echo "press ****return to continue.."
#read 

echo $sub >> $DATELOG;

echo 
echo "filling the chado db with $sub..."
stag-storenode.pl -D "Pg:$CHADODB@$DBHOST" -user $DBUSER -password\
 $DBPW -noupdate cvterm,dbxref,db,cv,feature $sub \
 || { printf "\n **** $sub **** stag-storenode FAILED at `date`.\n" "%b" \
 >> `date "+%y%m%d.$REL.log"`; grep -v $sub $DATELOG > tmp ; mv -f tmp $DATELOG; $F ; }

done

#echo "press return to continue.."
#read 

#---------------------------------------
# building modmine
#---------------------------------------
cd $MINEDIR

if [ $ONLYMETA = "y" ]
then
../bio/scripts/project_build -a $SOURCES -V $REL $V -b -t localhost /tmp/mod-meta\
 || { printf "%b" "\n modMine build FAILED.\n" ; exit 1 ; }
else
../bio/scripts/project_build -V $REL $V -b -t localhost /tmp/mod-all\
 || { printf "%b" "\n modMine build FAILED.\n" ; exit 1 ; }
fi

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
