#!/bin/bash
# 
# default usage: automine.sh rel
#
# note: you should put the db password in ~/.pgpass if don't
#       want to be prompted for it
#
# sc 09/08
#
# TODO: ant failing and exiting with 0!
#       #3 scenari: new build, incremental (i.e. new chado added to existing mine), test
#       test with file option
#       analyse after stag?
#       #-R restart after fails for full
#       
#



# see after argument parsing for all envs related to the release

FTPURL=ftp://ftp.modencode.org/pub/dcc/for_modmine
MODIR=/shared/data/modmine
DATADIR=$MODIR/subs/chado
NEWDIR=$DATADIR/new

MINEDIR=$HOME/svn/dev/modmine
SOURCES=modmine-static,modencode-metadata,entrez-organism

# these should not be edited
WEBAPP=y       #defaults: build a webapp
CHADOAPPEND=n  #          rebuild the chado db
BUP=n          #          don't do a back up copy of the databases
V=             #          non-verbose mode
#F=;           #          continue stag loading (also after errors)
REL=dev;       #          if no release is passed, do a dev
STAG=y         #          run stag loading
TESTS=y        #          do acceptance tests
FOUND=n        #          y if new files downloaded
INFILE=not_defined #   not using a given list of submissions
TIMESTAMP=`date "+%y%m%d.%H%M"`  # used in the log

INCR=y
FULL=n
TEST=n
RESTART=n


progname=$0

function usage () {
   cat <<EOF

Usage: $progname [-b] [-c] [-f file_name] [-n] [-s] [-t] [-w] [-v]
   -F: full (modmine) rebuild
   -T: test build
   -R: restart full build after failure
   -a: append to chado
   -b: build a back-up of modchado-$REL
   -f file_name: using a given list of submissions
   -s: no new loading of chado (stag is not run)
   -t: no acceptance test run
   -w: no new webapp will be built
   -v: verbode mode

 Note: The file is downloaded only if not present or the remote copy 
      is newer or has a different size. 

examples:

$progname
         build a modmine-dev with metadata only, getting new files from ftp
$progname -c test
         build a modmine-test with metadata, Flybase and Wormbase
$progname -n test
         build a new chado with all the NEW submissions in $FTPURL and use this to build a modmine-test
$progname -s -w -t  dev
         build modmine-dev using the existing modchado-dev, without performing acceptance tests and without building the webapp
$progname -f file_name val
         build modmine-val using the (already downloaded) chadoxml files listed in file_name


EOF
   exit 0
}

while getopts ":FITRbcf:nstvw" opt; do
   case $opt in

   F )  echo; echo "Full modMine realease"; FULL=y; BUP=y;;
#   I )  echo; echo "Incremental modMine realease"; INCR=y;;
   T )  echo; echo "Test realease"; TEST=y;;
   R )  echo; echo "Restart full realease"; RESTART=y;;
   b )  echo; echo "Build a back-up of the database." ; BUP=y;;
   f )  echo; INFILE=$OPTARG; echo "Using given list of chadoxml files:"; more $INFILE;;
   a )  echo; echo "Append data in chado" ; CHADOAPPEND=y;;
   s )  echo; echo "Using previous load of chado (stag is not run)" ; STAG=n; BUP=n;;
   t )  echo; echo "No acceptance test run" ; TESTS=n;;
   v )  echo; echo "Verbose mode" ; V=-v;;
   w )  echo; echo "No new webapp will be built" ; WEBAPP=n;;
   h )  usage ;;
   \?)  usage ;;
   esac
done

shift $(($OPTIND - 1))

if [ -n "$1" ]
then
REL=$1
fi

# if we are using the same chado, no chado back up will be created
if [ $STAG = "n" ]
then
BUP=n
fi


LOADLOG="$DATADIR/loading_$REL.log"

echo $LOADLOG

#
# Getting some values from the properties file.
# NOTE: it is assumed that dbhost and dbuser are the same for chado and modmine!!
#


DBHOST=`grep metadata.datasource.serverName $HOME/modmine.properties.$REL | awk -F "=" '{print $2}'`
DBUSER=`grep metadata.datasource.user $HOME/modmine.properties.$REL | awk -F "=" '{print $2}'`
DBPW=`grep metadata.datasource.password $HOME/modmine.properties.$REL | awk -F "=" '{print $2}'`
CHADODB=`grep metadata.datasource.databaseName $HOME/modmine.properties.$REL | awk -F "=" '{print $2}'`
MINEDB=`grep db.production.datasource.databaseName $HOME/modmine.properties.$REL | awk -F "=" '{print $2}'`


echo "================================="
echo "Building modmine-$REL on $DBHOST."
echo "================================="
echo "Logs: $LOADLOG"
echo "      $DATADIR/wget.log"
echo
echo "Press return to continue.."
echo -n "->"
read

#---------------------------------------
# getting the chadoxml from ftp site
#---------------------------------------

cd $DATADIR
# this for confirmation the program run and to avoid to grep on a non-existent file
touch $LOADLOG

#...and get it if the remote timestamp is newer than the local
# it will make a copy of the local (as name.chadoxml.n)

if [ $STAG = "y" ] && [ $INFILE = "not_defined" ]
then
#wget -N $FTPURL$DIR/*.chadoxml

echo
echo "Getting data from $FTPURL. Log in $DATADIR/wget.log"
echo
#wget -r -nd -N -P$NEWDIR $FTPURL -A chadoxml  --progress=dot:mega -a wget.log
wget -r -nd -N -P$NEWDIR $FTPURL -A chadoxml  --progress=dot:mega 2>&1 | tee -a $DATADIR/wget.log

#wget -r -nd -np -l 2 -N -P$NEWDIR $FTPURL/*chadoxml  #?? to test
#r recursive
#nd the directories structure is NOT recreated locally
#l 2 depth of recursion
#np no parents: only files below a certain directory are retrieved
#P destination dir
#a CHADOAPPEND to the log

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

# else read file, mv files to newdir and go
# nb: check clobbing, and if files already in newdir (not links)
elif [ !$INFILE = "not_defined" ]
then
for chadofile in `cat $INFILE`
do
echo "$chadofile..."
mv $chadofile $NEWDIR
done

fi #if $STAG=y

#---------------------------------------
# build the chado db
#---------------------------------------
#
cd $DATADIR


# do a back-up?

if [ "$BUP" = "y" ]
then
dropdb -e "$CHADODB"-old -h $DBHOST -U $DBUSER;
createdb -e "$CHADODB"-old -T $CHADODB -h $DBHOST -U $DBUSER\
|| { printf "%b" "\nMine building FAILED. Please check previous error message.\n\n" ; exit 1 ; }
fi

# build new?

if [ "$CHADOAPPEND" = "n" ] && [ "$STAG" = "y" ]
then
dropdb -e $CHADODB -h $DBHOST -U $DBUSER;
createdb -e $CHADODB -h $DBHOST -U $DBUSER || { printf "%b" "\nMine building FAILED. Please check previous error message.\n\n" ; exit 1 ; }

#echo "press return to continue.."
#read

psql -d $CHADODB -h $DBHOST -U $DBUSER < $MODIR/build_empty_chado.sql\
|| { printf "%b" "\nMine building FAILED. Please check previous error message.\n\n" ; exit 1 ; }
echo "press return to continue.."
read
fi

#---------------------------------------
# fill chado db
#---------------------------------------

if [ $STAG = "y" ]
then

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
 >> `date "+%y%m%d.$REL.log"`; grep -v $sub $LOADLOG > tmp ; mv -f tmp $LOADLOG; exit 1 ; }
# >> `date "+%y%m%d.$REL.log"`; $F ; }

mv $sub $DATADIR
ln -s ../$sub $sub
fi
done

else
echo
echo "Using previously loaded chado."
echo
fi # if $STAG=y

echo "press return to continue.."
read

#---------------------------------------
# build modmine
#---------------------------------------
cd $MINEDIR

echo "Building modMine $REL"
echo

if [ $INCR = "y" ]
then
# just add to present mine
# NB: if failing won't stop!! ant exit with 0!
echo; echo "Appending new chado (metadata) to modmine-$REL.."
cd integrate
ant -v -Drelease=$REL -Dsource=modencode-metadata || { printf "%b" "\n modMine build FAILED.\n" ; exit 1 ; }
elif [ $RESTART = "y" ]
then
# restart build after failure
echo; echo "Restating build.."
../bio/scripts/project_build -V $REL $V -l -t localhost /tmp/mod-all\
 || { printf "%b" "\n modMine build FAILED.\n" ; exit 1 ; }
elif [ $TEST = "y" ]
then
# new build. static, metadata, organism
../bio/scripts/project_build -a $SOURCES -V $REL $V -b -t localhost /tmp/mod-meta\
 || { printf "%b" "\n modMine build FAILED.\n" ; exit 1 ; }
else
# new build, all the sources
../bio/scripts/project_build -V $REL $V -b -t localhost /tmp/mod-all\
 || { printf "%b" "\n modMine build FAILED.\n" ; exit 1 ; }
fi

# if [ $CHADOAPPEND = "y" ]
# then
# # just add to present mine
# # NB: if failing won't stop!! ant exit with 0!
# cd integrate
# ant -v -Drelease=$REL -Dsource=modencode-metadata || { printf "%b" "\n modMine build FAILED.\n" ; exit 1 ; }
# elif [ $ONLYMETA = "y" ]
# then
# # new build. static, metadata, organism
# ../bio/scripts/project_build -a $SOURCES -V $REL $V -b -t localhost /tmp/mod-meta\
#  || { printf "%b" "\n modMine build FAILED.\n" ; exit 1 ; }
# else
# # new build, all the sources
# ../bio/scripts/project_build -V $REL $V -b -t localhost /tmp/mod-all\
#  || { printf "%b" "\n modMine build FAILED.\n" ; exit 1 ; }
# fi

echo "press return to continue.."
read

#---------------------------------------
# building webapp
#---------------------------------------
if [ "$WEBAPP" = "y" ]
then
cd $MINEDIR/webapp
ant -Drelease=$REL $V default remove-webapp release-webapp
fi

echo "press return to continue.."
read

#---------------------------------------
# and run acceptance tests
#---------------------------------------
if [ "$TESTS" = "y" ]
then
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

