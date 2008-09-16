#!/bin/bash
# 
# default usage: domodmine.sh
#
# note: you should put the db password in ~/.pgpass if don't
#       want to be prompted for it
#
# sc 09/08
#
# TODO: use wget -r
#       better error logging 
#

DBHOST=bert
DBUSER=sc
DBPW=sc

FTPURL=ftp://ftp.modencode.org/pub/dcc
DATADIR=/shared/data/modmine/subs/chado
DBDIR=/shared/data/modmine/
#CHADODB=modchado-$REL
#MINEDB=modmine-$REL
DATELOG=loading_times.log

MINEDIR=~/svn/dev/modmine
SOURCES=modencode-static,entrez-organism,modencode-metadata

# these should not be edited
WEBAPP=y;   #defaults: build a webapp
APPEND=n;   #          incremental loading
BUP=y;      #          do a back up copy of the modchado database       
V=;         #          non-verbose mode
F=;         #          continue stag loading (also after errors)
DIR=

progname=$0

function usage () {
   cat <<EOF

Usage: $progname [-a] [-u] [-n] [-d directory_name] [-v] [-f] 
   -a: the submission will be APPENDED to the present validation mine
   -n: no new webapp will be built
   -u: no back-up of modchado-$REL will be built
   -v: verbode mode
   -f: force building of mine after a failure in the loading in chado 
   
Notes: The file is downloaded only if not present or the remote copy 
      is newer (in this case a copy of the older should be created)
      
example
       $progname db_instance {dev/test}
  
EOF
   exit 0
}

while getopts ":anud:v" opt; do
   case $opt in

   a )  echo; echo "Append to exinting mine." ; APPEND=y;;
   u )  echo; echo "Don't build a back-up of the database." ; BUP=n;;
   d )  echo "processing $FTPURL/$OPTARG directory" ; DIR=/$OPTARG;;
   n )  echo; echo "No new webapp will be built" ; WEBAPP=n;;
#   f )  echo; echo "Forcing mode: will continue if loading in chado gives errors." ; F=;;
   v )  echo; echo "Verbose mode" ; V=-v;;
   h )  usage ;;
   \?)  usage ;;
   esac
done

shift $(($OPTIND - 1))
#echo "the remaining arguments are: $1 $2 $3"

REL=$1
CHADODB=modchado-$REL
MINEDB=modmine-$REL

echo $REL
echo $DIR
#echo
echo "press return to load $1.."
read 

#---------------------------------------
# getting the chadoxml from ftp site
#---------------------------------------

cd $DATADIR

#...and get it if the remote timestamp is newer than the local
# it will make a copy of the local (as name.chadoxml.n)
#wget -N $FTPURL$DIR/*.chadoxml

echo "press return to continue.."
read 

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
# do check of previous loads (if only update)

#if [ $APPEND=y ]
#then
#if [ $sub -ot $DATELOG ] 
#then break 
#fi
#fi
echo ...$sub

if [ $APPEND = "y" ] && [ "$sub" -ot "$DATELOG" ]
then
 break 
fi

stag-storenode.pl -D "Pg:$CHADODB@$DBHOST" -user $DBUSER -password\
 $DBPW -noupdate cvterm,dbxref,db,cv,feature $sub \
 || { printf "\n **** $sub **** stag-storenode FAILED at `date`.\n" "%b" \
 >> `date "+%y%m%d.log"`; $F ; }

echo $sub >> $DATELOG;

done

echo "press return to continue.."
read 


#---------------------------------------
# building modmine
#---------------------------------------

cd $MINEDIR

../bio/scripts/project_build -a $SOURCES -V $REL $V -b -t localhost /tmp/mod-meta\
 || { printf "%b" "\n modMine build FAILED.\n" ; exit 1 ; }

echo "press return to continue.."
read 


#---------------------------------------
# building webapp
#---------------------------------------

if [ "$WEBAPP" == "y" ]
then
cd $MINEDIR/webapp
ant -Drelease=$REL $V default remove-webapp release-webapp
fi
