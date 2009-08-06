#!/bin/bash
#
# default usage: automine.sh
#
# note: you should put the db password in ~/.pgpass if don't
#       want to be prompted for it
#
# sc 09/08
#
# TODO: ant failing and exiting with 0!
#       if -M -f stops after stag. check
#       improve logs!!
#

# see after argument parsing for all envs related to the release

FTPURL=http://submit.modencode.org/submit/public
SUBDIR=/shared/data/modmine/subs
REPORTS=$SUBDIR/reports
DATADIR=$SUBDIR/chado
FAILDIR=$DATADIR/chado/new/failed
PROPDIR=$HOME/.intermine
SCRIPTDIR=../flymine-private/scripts/modmine/

RECIPIENTS=contrino@flymine.org,rns@flymine.org
#SOURCES=modmine-static,modencode-metadata,entrez-organism
SOURCES=modmine-static,modencode-metadata
#SOURCES=modencode-metadata

# set minedir and check that modmine in path
MINEDIR=$PWD
pwd | grep modmine > pathcheck.tmp
if [ ! -s pathcheck.tmp ]
then
echo "EXITING: you should be in the modmine directory from your checkout."
echo
rm pathcheck.tmp
exit;
fi

rm pathcheck.tmp

BUILDDIR=$MINEDIR/integrate/build

# default settings: edit with care
WEBAPP=y         # build a webapp
BUILD=y          # build modmine
CHADOAPPEND=n    # rebuild the chado db
BUP=n            # don't do a back up copy of the databases
V=               # non-verbose mode
REL=dev;         # if no release is passed, do a dev (unless validating or making a full release)
STAG=y           # run stag loading
TEST=y           # do acceptance tests
VALIDATING=n     # not running as a validation
FOUND=n          # y if new files downloaded
INFILE=undefined # not using a given list of submissions
INTERACT=n       # y: step by step interaction
WGET=y           # use wget to get files from ftp
GAM=y            # run get_all_modmine (only in F mode)
STOP=n           # y if warning in the setting of the directories for chado.
STAGFAIL=n       # y if stag failed. when validating, we skip the failed sub and continue

# these are mutually exclusive
# should be enforced
INCR=y
FULL=n
META=n           # it builds a new mine with static and metadata only
RESTART=n

progname=$0

function usage () {
	cat <<EOF

Usage:
$progname [-F] [-M] [-R] [-V] [-f file_name] [-g] [-i] [-r release] [-s] [-v] DCCid
	-F: full (modmine) rebuild (Uses modmine-build as default)
	-M: test build (metadata only)
	-R: restart full build after failure
	-V: validation mode: all new entries,one at the time (Uses modmine-val as default)
	-f file_name: using a given list of submissions
	-g: no checking of ftp directory (wget is not run)
	-i: interactive mode
	-r release: specify which instance to use (val, dev, build). Default is dev (not with -V or -F)
	-s: no new loading of chado (stag is not run)
	-v: verbode mode

In addition to those:
Advanced Usage switches: [-a] [-b] [-e] [-t] [-w] [-x]
	-a: append to chado
	-b: don't build a back-up of modchado-$REL
	-e: don't update the sources (don't run get_all_modmine). Valid only in F mode
	-t: no acceptance test run
	-w: no new webapp will be built
	-x: don't build modmine (!: used for running only tests)


Parameters: you can process
            a single submission                   (e.g. automine.sh 204 )
						a list of submission in an input file (e.g. automine.sh -V -f infile )
            all the available submissions         (e.g. automine.sh -F )

            you can also pass the release, overwriting the default
                                                  (e.g. automine.sh -F -r test )

Notes: The file is downloaded only if not present or the remote copy
			 is newer or has a different size.
 
       If no uppercase switch is used (V, M, F, R), the submissions found in the relevant chado
       (default modchado-dev) are ADDED to the relevant modmine (default: modmine-dev)

examples:

$progname			add new submissions to modmine-dev, getting new files from ftp
$programe 123 add submission 123 to modmine-dev, getting it from ftp
$progname -F -r test		build a modmine-test with metadata, Flybase and Wormbase,
				getting new files from ftp
$progname -M -r test		build a new chado with all the NEW submissions in
				$FTPURL
				and use this to build a modmine-test
$progname -s -w -t build modmine-dev using the existing modchado-dev,
				without performing acceptance tests and without building the webapp
$progname -f file_name -r val	build modmine-val using the submissions listed in file_name

EOF
	exit 0
}

while getopts ":FMRVabef:gir:stvwx" opt; do
	case $opt in

	F )  echo; echo "Full modMine realease"; FULL=y; BUP=y; INCR=n; REL=build;;
	M )  echo; echo "Test build (metadata only)"; META=y; INCR=n;;
	R )  echo; echo "Restart full realease"; RESTART=y; FULL=y; INCR=n; STAG=n; WGET=n; BUP=n; REL=build;;
	V )  echo; echo "Validating submission(s) in $DATADIR/new"; VALIDATING=y; META=y; INCR=n; BUP=n; REL=val;;
	a )  echo; echo "Append data in chado" ; CHADOAPPEND=y;;
	b )  echo; echo "Don't build a back-up of the database." ; BUP=n;;
	e )  echo; echo "don't update all sources (get_all_modmine is not run)" ; GAM=n;;
	f )  echo; INFILE=$OPTARG; echo "Using given list of chadoxml files: "; more $INFILE;;
	g )  echo; echo "No checking of ftp directory (wget is not run)" ; WGET=n;;
	i )  echo; echo "Interactive mode" ; INTERACT=y;;
	r )  echo; REL=$OPTARG; echo "Using release $REL";;
	s )  echo; echo "Using previous load of chado (stag is not run)" ; STAG=n; BUP=n; WGET=n;;
	t )  echo; echo "No acceptance test run" ; TEST=n;;
	v )  echo; echo "Verbose mode" ; V=-v;;
	w )  echo; echo "No new webapp will be built" ; WEBAPP=n;;
	x )  echo; echo "modMine will NOT be built" ; BUILD=n; STAG=n; BUP=n; WGET=n;;
	h )  usage ;;
	\?)  usage ;;
	esac
done

shift $(($OPTIND - 1))

#
# Getting some values from the properties file.
# NOTE: it is assumed that dbhost and dbuser are the same for chado and modmine!!
#     -a to grep also (alleged) binary files
#

DBHOST=`grep -a metadata.datasource.serverName $PROPDIR/modmine.properties.$REL | awk -F "=" '{print $2}'`
DBUSER=`grep -a metadata.datasource.user $PROPDIR/modmine.properties.$REL | awk -F "=" '{print $2}'`
DBPW=`grep -a metadata.datasource.password $PROPDIR/modmine.properties.$REL | awk -F "=" '{print $2}'`
CHADODB=`grep -a metadata.datasource.databaseName $PROPDIR/modmine.properties.$REL | awk -F "=" '{print $2}'`
MINEDB=`grep -a db.production.datasource.databaseName $PROPDIR/modmine.properties.$REL | awk -F "=" '{print $2}'`

LOG="$DATADIR/$USER.$REL."`date "+%y%m%d.%H%M"`  # timestamp of stag operations + error log

echo
echo "================================="
echo "Building modmine-$REL on $DBHOST."
echo "================================="
echo "Log: $LOG"
echo
echo "current directory: $MINEDIR"
echo

# TODO: actually only read n ...
function interact {
# if testing, wait here before continuing
if [ $INTERACT = "y" -o $STOP = "y" ]
then
echo
echo "Press return to continue (^C to exit).."
echo -n "->"
read
fi

}

function chadorebuild {
# rebuild skeleton chado db
RETURNDIR=$PWD
cd $MINEDIR
dropdb -e $CHADODB -h $DBHOST -U $DBUSER;
createdb -e $CHADODB -h $DBHOST -U $DBUSER || { printf "%b" "\nMine building FAILED. Please check previous error message.\n\n" ; exit 1 ; }
# initialise it
psql -q -o /dev/null -d  $CHADODB -h $DBHOST -U $DBUSER < $SCRIPTDIR/build_empty_chado.sql\
|| { printf "%b" "\nMine building FAILED. Please check previous error message.\n\n" ; exit 1 ; }
cd $RETURNDIR
}

function runtest {
# run the relevant acceptance tests and name the report
# with dccid if validating
# with $REL_timestamp otherwise
echo
echo "========================"
echo "running acceptance tests"
echo "========================"
echo
cd $MINEDIR/integrate

if [ $FULL = "y" ]
then
ant $V -Drelease=$REL acceptance-tests|| { printf "%b" "\n acceptance test FAILED.\n" ; exit 1 ; }
else
ant $V -Drelease=$REL acceptance-tests-metadata|| { printf "%b" "\n acceptance test FAILED.\n" ; exit 1 ; }
fi

# check chado for new features
# this is done because there is a problem with automount....
ls $REPORTS > /dev/null

cd $MINEDIR
$SCRIPTDIR/add_chado_feats_to_report.pl $DBHOST $CHADODB $DBUSER $BUILDDIR/acceptance_test.html > $REPORTS/$1.html

echo "sending mail!!"
mail $RECIPIENTS -s "$1 report, also in file://$REPORTS/$1.html" < $REPORTS/$1.html
#elinks $REPORTS/$1.html
echo
echo "acceptance test results in "
echo "$REPORTS/$1.html"
echo
}

function dcczip {
# in a directory, look for all the .chadoxml files that are compressed
# i.e. newly downloaded. Depending on their location, they will be moved in the
# proper directory (new|update), which is the argument of the function
for sub in *.chadoxml
do
    file $sub | grep compressed > bintest
    if [ -s bintest ]
     then
# unzip and rename dowloaded file
DCCID=`echo $sub | cut -f 1 -d.`
echo "unzipping updated file $DCCID"
gzip -S .chadoxml -d $sub
      mv $DCCID $DATADIR/$1/$sub
  		FOUND=y
    fi
done
}

function chadofill {
# run stag and add the dccid
echo
echo "filling $CHADODB db with $1..."
echo "STAG: `date "+%y%m%d.%H%M"` $1" >> $LOG

DCCID=`echo $1 | cut -f 1 -d.`

# stag-storenode.pl -D "Pg:$CHADODB@$DBHOST" -user $DBUSER -password \
# $DBPW -noupdate cvterm,dbxref,db,cv,feature $1 \
# || { printf "\n$1  stag-storenode FAILED. EXITING. \n\n" "%b" ; exit 1 ; }

stag-storenode.pl -D "Pg:$CHADODB@$DBHOST" -user $DBUSER -password \
$DBPW -noupdate cvterm,dbxref,db,cv,feature $1 

exitstatus=$?

if [ "$exitstatus" = "0" ]
then
psql -h $DBHOST -d $CHADODB -U $DBUSER -c "insert into experiment_prop (experiment_id, name, value, type_id) select max(experiment_id), 'dcc_id', '$DCCID', 1292 from experiment_prop;"
else
echo "\n$1  stag-storenode FAILED. SKIPPING SUBMISSION. \n\n"
STAGFAIL=y
fi

}

interact

#---------------------------------------
# getting the chadoxml from ftp site
#---------------------------------------

if [ "$WGET" = "y" ]
then
		echo
		echo "Getting data from $FTPURL. Log in $DATADIR/wget.log"
		echo

#cd $DATADIR
cd $DATADIR/new
# this for confirmation the program run and to avoid to grep on a non-existent file
touch $LOG

#FTPURL=http://submit.modencode.org/submit/public/
# NB: files are dowloaded gzipped: see next loop for decompression
#
#  a append to log
# -N timestamping
# -t number of tries

if [ -n "$1" ]
then
LOOPVAR="$1"
elif [ $INFILE != "undefined" ]
then
# use the list provided in a file
LOOPVAR=`cat $INFILE`
else
# get the full list from the ftp site and save it for reference
wget -O - $FTPURL/list.txt | sort > $DATADIR/loft/`date "+%y%m%d"`.list
# get the list of live dccid and use it as loop variable
grep released $DATADIR/loft/`date "+%y%m%d"`.list | grep false | awk '{print $1}' > $DATADIR/live.dccid
LOOPVAR=`cat $DATADIR/live.dccid`
# get also the list of deprecated entries with their replacement
grep released $DATADIR/loft/`date "+%y%m%d"`.list | grep true | awk '{print $1, " -> ", $3 }' > $DATADIR/deprecation.table

awk '{print $1}' $DATADIR/deprecation.table > $DATADIR/dead.dccid

# wget -O - $FTPURL/list.txt | grep released | grep false | awk '{print $1}' | sort > $DATADIR/live.dccid
# LOOPVAR=`cat $DATADIR/live.dccid`
# 
# # get also the list of deprecated entries with their replacement
# wget -O - $FTPURL/list.txt | grep released | grep true | awk '{print $1, " -> ", $3 }' | sort > $DATADIR/deprecated.dccid

fi

for sub in $LOOPVAR
do
 wget -t3 -N --header="accept-encoding: gzip" $FTPURL/get_file/$sub/extracted/$sub.chadoxml  --progress=dot:mega 2>&1 | tee -a $DATADIR/wget.log
done


#-------------------------------------------------
# check if any NEW file, decompress and rename it
#-------------------------------------------------
# arg: the destination directory
dcczip new

#-------------------------------------------------
# check if any UPDATED file, decompress and rename it
#-------------------------------------------------
cd $DATADIR
dcczip update

#------------------------------------------------------------------------
# check if any update in the ERR directory, decompress, mv and rename it
#------------------------------------------------------------------------
cd $DATADIR/new/err
dcczip new

if [ "$FOUND" = "n" ]
then
	echo
	echo "no new data found on ftp. exiting."
	echo
	exit 0;
fi

#------------------------------------------------------
# prepare directories for stag in case of FULL release
#------------------------------------------------------
# TODO: if no $1, no infile, no incr -> do all
    if [ "$FULL" = "y" ]
     then
      cd $DATADIR
      mv *.chadoxml $DATADIR/new
      mv $DATADIR/update/*.chadoxml $DATADIR/new
      mv $DATADIR/new/validated/*.chadoxml $DATADIR/new
      cd $DATADIR/new
   		for sub in *.chadoxml 
   		do
       # if found symbolic link not to err directory throw an error
       if [ -L "$sub" -a ! -e "$DATADIR/new/err/$sub" ]
        then
        echo "WARNING: $sub is missing from load directory" | tee -a $LOG
			  STOP=y # TODO: if incr you should exit! or exit always
       fi
		  done
    fi

interact
cd $DATADIR
fi #if $WGET=y

#---------------------------------------
# build the chado db
#---------------------------------------
#

# do a back-up?
if [ "$BUP" = "y" ]
then
dropdb -e "$CHADODB"-old -h $DBHOST -U $DBUSER;
createdb -e "$CHADODB"-old -T $CHADODB -h $DBHOST -U $DBUSER\
|| { printf "%b" "\nMine building FAILED. Please check previous error message.\n\n" ; exit 1 ; }
fi

# build new?
 if [ "$CHADOAPPEND" = "n" ] && [ "$STAG" = "y" ] && [ "$VALIDATING" = "n" ]
 then
chadorebuild
interact 
 fi

#---------------------------------------
# fill chado db
#---------------------------------------

if [ "$STAG" = "y" ]
then

if [ -n "$1" ]
then
LOOPVAR="$1.chadoxml"
elif [ $INFILE != "undefined" ]
then
# use the list provided in a file
#LOOPVAR=`cat $INFILE`
LOOPVAR=`sed 's/$/.chadoxml/g' $INFILE | cat`
echo "********"
echo $LOOPVAR

else
LOOPVAR="*.chadoxml"
fi

cd $DATADIR/new

for sub in $LOOPVAR
do
# if it is a symbolic link and this is not the given input
# we skip that file
#if [ -L "$sub" -a ! -n "$1" ]
if [ -L "$sub" -a "$LOOPVAR" = "*.chadoxml" ]
then
continue
fi

echo "================"
echo "$sub..."
echo "================"

#
# for validation, we rebuild chado for each file
#
if [ "$CHADOAPPEND" = "n" ] && [ "$VALIDATING" = "y" ]
then
chadorebuild
fi

chadofill $sub

if [ "$STAGFAIL" = "y" ]
then
mv $sub $FAILDIR
STAGFAIL=n
continue
fi

# if building the release, we move the file
if [ "$FULL" = "y" ]
then
mv $sub $DATADIR
ln -s ../$sub $sub
fi

#if we are validating, we'll process an entry at a time
if [ "$VALIDATING" = "y" ]
then

cd $MINEDIR
echo "Building modMine $REL"
echo
# new build. static, metadata
../bio/scripts/project_build -a $SOURCES -V $REL $V -b -t localhost /tmp/mod-meta\
|| { printf "%b" "\n modMine build FAILED.\n" ; exit 1 ; }

# to name the acceptance tests file
NAMESTAMP=`echo $sub | awk -F "." '{print $1}'`
runtest $NAMESTAMP


# go back to the chado directory and mv chado file in 'done'
# this is to allow to run the validation as a cronjob
cd $DATADIR/new
mv $sub validated
cp -s validated/$sub .
fi #VAL=y

done

# if we are validating, that's all
if [ "$VALIDATING" = "y" ]
then
exit;
fi

else
echo
echo "Using previously loaded chado."
echo
fi # if $STAG=y

interact

#---------------------------------------
# build modmine
#---------------------------------------
if [ $BUILD = "y" ]
then
cd $MINEDIR
echo
echo "Building modMine $REL"
echo

if [ $INCR = "y" ]
then
# just add to present mine
# NB: if failing won't stop!! ant exit with 0!
echo; echo "Appending new chado (metadata) to modmine-$REL.."
cd integrate
ant $V -Drelease=$REL -Dsource=modencode-metadata-inc || { printf "%b" "\n modMine build FAILED.\n" ; exit 1 ; }
elif [ $RESTART = "y" ]
then
# restart build after failure
echo; echo "Restating build.."
../bio/scripts/project_build -V $REL $V -l -t localhost /tmp/mod-all\
|| { printf "%b" "\n modMine build (restart) FAILED.\n" ; exit 1 ; }
elif [ $META = "y" ]
then
# new build. static, metadata, organism
../bio/scripts/project_build -a $SOURCES -V $REL $V -b -t localhost /tmp/mod-meta\
|| { printf "%b" "\n modMine build (only metadata) FAILED.\n" ; exit 1 ; }
else
# new build, all the sources
# get the most up to date sources ..
if [ $GAM = "y" ]
then
cd ../bio/scripts
./get_all_modmine.sh|| { printf "%b" "\n modMine build (get_all_modmine.sh) FAILED.\n" ; exit 1 ; }
fi
# .. and build modmine
cd $MINEDIR
../bio/scripts/project_build -V $REL $V -b -t localhost /tmp/mod-all\
|| { printf "%b" "\n modMine build FAILED.\n" ; exit 1 ; }
fi

else
echo
echo "Using previously built modMine."
echo
fi #BUILD=y

interact

#---------------------------------------
# building webapp
#---------------------------------------
if [ "$WEBAPP" = "y" ]
then
cd $MINEDIR/webapp
ant -Drelease=$REL $V default remove-webapp release-webapp
fi

interact

#---------------------------------------
# and run acceptance tests
#---------------------------------------
if [ "$TEST" = "y" ] && [ $VALIDATING = "n" ]
then
NAMESTAMP="$REL"_`date "+%y%m%d.%H%M"`
runtest $NAMESTAMP
fi

