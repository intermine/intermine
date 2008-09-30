#!/bin/bash
# 
# default usage: domodmine.sh rel
#
# note: you should put the db password in ~/.pgpass if don't
#       want to be prompted for it
#
# sc 09/08
#
# TODO: better error logging
#

# see after argument parsing for all envs related to the release

FTPURL=ftp://ftp.modencode.org/pub/dcc
DATADIR=/shared/data/modmine/subs/chado
NEWDIR=/shared/data/modmine/subs/chado/new
DBDIR=/shared/data/modmine/

MINEDIR=$HOME/svn/dev/modmine
SOURCES=modencode-static,entrez-organism,modencode-metadata

# these should not be edited
WEBAPP=n;   #defaults: don't build a webapp
APPEND=n;   #          rebuild the db
BUP=n;      #          don't do a back up copy of the modchado database
V=;         #          non-verbose mode
F=;         #          continue stag loading (also after errors)
REL=auto;   #          if no release is passed, do a dev
ONLYMETA=y  #          do only metadata
TESTS=y     #          do acceptance teests
FOUND=n     #          y if new files downloaded
TIMESTAMP=`date "+%y%m%d.%H%M"`  # t


progname=$0

function usage () {
   cat <<EOF

Usage: $progname [-a] [-b] [-c] [-w] [-v]
   -a: the submission will be APPENDED to the present mine
   -b: no back-up of modchado-$REL will be built
   -c: all data, not only meta-data (FB and WB)
   -w: build webapp
   -v: verbode mode
   
 Note: The file is downloaded only if not present or the remote copy 
      is newer or has a different size. 
      
examples:

$progname
         will build a modmine-auto with metadata only
$progname -c test
         will build a modmine-test with metadata, Flybase and Wormbase
$progname -a -d ready2publish dev
         will add to modmine-dev all the new submissions in $FTPURL/ready2publish
$progname -a test
         will add to modmine-test all the new submissions in $FTPURL

EOF
   exit 0
}

while getopts ":abctwv" opt; do
   case $opt in

   a )  echo; echo "Append to exinting mine." ; APPEND=y;;
   b )  echo; echo "Build a back-up of the database." ; BUP=y;;
   c )  echo; echo "Do all (not only meta-data)." ; ONLYMETA=n;;
   t )  echo; echo "No acceptance test run" ; TESTS=n;;
   w )  echo; echo "The webapp will be built" ; WEBAPP=n;;
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
LOADLOG=loading_$REL.log

DBHOST=`grep metadata.datasource.serverName $HOME/modmine.properties.$REL | awk -F "=" '{print $2}'`
DBUSER=`grep metadata.datasource.user $HOME/modmine.properties.$REL | awk -F "=" '{print $2}'`
DBPW=`grep metadata.datasource.password $HOME/modmine.properties.$REL | awk -F "=" '{print $2}'`


# echo
# echo "building modmine-$REL on $DBHOST.."
# read

#---------------------------------------
# getting the chadoxml from ftp site
#---------------------------------------

cd $DATADIR
# this for confirmation the program run and to avoid to grep on a non-existent file
touch $LOADLOG

#...and get it if the remote timestamp is newer than the local
# it will make a copy of the local (as name.chadoxml.n)

#wget -N $FTPURL$DIR/*.chadoxml

wget -r -nd -N -P$NEWDIR $FTPURL -A chadoxml  --progress=dot:mega -a wget.log

#wget -r -nd -np -l 2 -N -P$NEWDIR $FTPURL/*chadoxml  #?? to test

#r recursive
#nd the directories structure is NOT recreated locally
#l 2 depth of recursion
#np no parents: only files below a certain directory are retrieved
#P destination dir
#a append to the log

echo $TIMESTAMP

echo "press return to continue.."
read

#---------------------------------------
# check if any new file, exit if not
#---------------------------------------

cd $NEWDIR

for sub in *.chadoxml
do
if [ ! -L $sub ] #is not a symbolic link
then
FOUND=y
break
fi
done

if [ "$FOUND" = "n" ]
then
echo
echo "no new data found on ftp. exiting."
echo

exit 0;
fi


#---------------------------------------
# build the chado db
#---------------------------------------
#
cd $DATADIR

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
# fill chado db
#---------------------------------------
echo 
echo "filling chado db ..."

cd $NEWDIR

for sub in *.chadoxml
do
if [ -L $sub ] #is a symbolic link
then
continue
else

echo 
echo "filling the chado db with $sub..."
echo "`date "+%y%m%d.%H%M"` $sub" >> $LOADLOG

stag-storenode.pl -D "Pg:$CHADODB@$DBHOST" -user $DBUSER -password\
 $DBPW -noupdate cvterm,dbxref,db,cv,feature $sub \
 || { printf "\n **** $sub **** stag-storenode FAILED at `date`.\n" "%b" \
 >> `date "+%y%m%d.$REL.log"`; grep -v $sub $LOADLOG > tmp ; mv -f tmp $LOADLOG; $F ; }
# >> `date "+%y%m%d.$REL.log"`; $F ; }

mv $sub $DATADIR
ln -s ../$sub $sub
fi
done

#echo "press return to continue.."
#read

#---------------------------------------
# build modmine
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

mv $MINEDIR/integrate/build/acceptance_test.html $MINEDIR/integrate/build/$TIMESTAMP.html
#xterm -bg grey20 -hold -e "elinks file://$MINEDIR/integrate/build/$1.html" &
elinks $MINEDIR/integrate/build/$TIMESTAMP.html

echo
echo "acceptance test results in "
echo "$MINEDIR/integrate/build/$TIMESTAMP.html"
echo
fi

#---------------------------------------
# building webapp
#---------------------------------------
if [ "$WEBAPP" = "y" ]
then
cd $MINEDIR/webapp
ant -Drelease=$REL $V default remove-webapp release-webapp
fi
