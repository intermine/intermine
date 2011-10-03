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
#       clean up. check directory preparation
#       improve logs!!
#       add switch logic / checks
#
# TODO 2: when validating, builds twice the chadodb at the beginning

# see after argument parsing for all envs related to the release

SUBDIR=/micklem/data/modmine/subs
DATADIR=$SUBDIR/chado
REPORTS=$SUBDIR/reports

MIRROR=$DATADIR/mirror 
LOADDIR=$DATADIR/load
LOGDIR=$DATADIR/logs
FTPARK=$DATADIR/ark/ftplist

PATCHDIR=$LOADDIR/patches

FTPURL=http://submit.modencode.org/submit/public
PROPDIR=$HOME/.intermine
SCRIPTDIR=../bio/scripts/flymine/modmine/

ARKDIR=/micklem/releases/modmine

RECIPIENTS=contrino@flymine.org,rns@flymine.org

# set minedir and check that modmine in path
MINEDIR=$PWD
BUILDDIR=$MINEDIR/integrate/build

#--rm?
pwd | grep modmine > pathcheck.tmp
if [ ! -s pathcheck.tmp ]
then
echo "EXITING: you should be in the modmine directory from your checkout."
echo
rm pathcheck.tmp
exit;
fi
rm pathcheck.tmp
#--mr


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
INFILE=          # not using a given list of submissions
INTERACT=n       # y: step by step interaction
WGET=y           # use wget to get files from ftp
PREP4FULL=n      # don't run get_all_modmine (only in F mode)
STOP=n           # y if warning in the setting of the directories for chado.
STAGFAIL=n       # y if stag failed. when validating, we skip the failed sub and continue
P=               # no project declared

# these are mutually exclusive
# should be enforced
INCR=y
FULL=n
META=n           # it builds a new mine with static and metadata only
RESTART=n        # restart building recovering last dumped db
QRESTART=n       # restart building using current db

progname=$0

function usage () {
	cat <<EOF

Usage:
$progname [-F] [-M] [-R] [-V] [-P] [-T] [-f file_name] [-g] [-i] [-r release] [-s] [-v] DCCid
	-F: full (modmine) rebuild (Uses modmine-build as default)
	-M: test build (metadata only)
	-R: restart full build after failure
	-V: validation mode: all new entries,one at the time (Uses modmine-val as default)
  -P project_name: as -M, but restricted to a project.
  -L list of projects: as -M, but using a (comma separated) list of projects.
	-f file_name: using a given list of submissions
	-g: no checking of ftp directory (wget is not run)
	-i: interactive mode
	-r release: specify which instance to use (val, dev, test, build). Default is dev (not with -V or -F)
	-s: no new loading of chado (stag is not run)
	-v: verbode mode

In addition to those:
Advanced Usage switches: [-a] [-b] [-p] [-t] [-w] [-x]
	-a: append to chado
	-b: don't build a back-up of modchado-$REL
	-p: prepare chadoxml directories and update the sources (run get_all_modmine). Valid only in F mode
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
$progname 123 add submission 123 to modmine-dev, getting it from ftp
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

echo

while getopts ":FMRQVP:L:abf:gipr:stvwx" opt; do
	case $opt in

#	F )  echo; echo "Full modMine realease"; FULL=y; BUP=y; INCR=n; REL=build;;
	F )  echo "- Full modMine realease"; FULL=y; INCR=n; REL=build;;
	M )  echo "- Test build (metadata only)"; META=y; INCR=n;;
	R )  echo "- Restart full realease"; RESTART=y; FULL=y; INCR=n; STAG=n; WGET=n; BUP=n; REL=build;;
	Q )  echo "- Quick restart full realease"; QRESTART=y; FULL=y; INCR=n; STAG=n; WGET=n; BUP=n; REL=build;;
	V )  echo "- Validating submission(s) in $DATADIR/new"; VALIDATING=y; META=y; INCR=n; BUP=n; REL=val;;
	P )  P=$OPTARG; META=y; INCR=n; P="`echo $P|tr '[A-Z]' '[a-z]'`"; echo "- Test build (metadata only) with project $P";;
	L )  L=$OPTARG; META=y; INCR=n; L="`echo $L|tr '[A-Z]' '[a-z]'`"; echo "- Test build (metadata only) with projects $L";;
	a )  echo "- Append data in chado" ; CHADOAPPEND=y;;
	b )  echo "- Don't build a back-up of the database." ; BUP=n;;
	p )  echo "- prepare directories for full realease and update all sources (get_all_modmine is run)" ; PREP4FULL=y;;
	f )  INFILE=$OPTARG; echo "- Using given list of chadoxml files: "; SHOW="`cat $INFILE|tr '[\n]' '[,]'`"; echo $SHOW;;
	g )  echo "- No checking of ftp directory (wget is not run)" ; WGET=n;;
	i )  echo "- Interactive mode" ; INTERACT=y;;
	r )  REL=$OPTARG; echo "- Using release $REL";;
	s )  echo "- Using previous load of chado (stag is not run)" ; STAG=n; BUP=n; WGET=n;;
	t )  echo "- No acceptance test run" ; TEST=n;;
	v )  echo "- Verbose mode" ; V=-v;;
	w )  echo "- No new webapp will be built" ; WEBAPP=n;;
	x )  echo "- modMine will NOT be built" ; BUILD=n; BUP=n;;
	h )  usage ;;
	\?)  usage ;;
	esac
done

shift $(($OPTIND - 1))

#
# NOTE: all modencode sources are supposed to be on the same server, etc.
# Getting some values from the properties file.
# -m1 to grep only the first occurrence (multiple modencode sources)
#

MINEHOST=`grep -v "#" $PROPDIR/modmine.properties.$REL | grep -m1 production.datasource.serverName | awk -F "=" '{print $2}'`
DBUSER=`grep -v "#" $PROPDIR/modmine.properties.$REL | grep -m1 metadata.datasource.user | awk -F "=" '{print $2}'`
DBPW=`grep -v "#" $PROPDIR/modmine.properties.$REL | grep -m1 metadata.datasource.password | awk -F "=" '{print $2}'`
MINEDB=`grep -v "#" $PROPDIR/modmine.properties.$REL | grep -m1 db.production.datasource.databaseName | awk -F "=" '{print $2}'`

# CHADODB becomes fixed for a given project
if [ -n "$P" ]
then
CHADODB="modchado-$P"
echo "- Single project: $P"
DBHOST=`grep -v "#" $PROPDIR/modmine.properties.$REL | grep metadata.datasource.serverName  | grep -w $P | awk -F "=" '{print $2}'`
else
DBHOST=`grep -v "#" $PROPDIR/modmine.properties.$REL | grep -m1 metadata.datasource.serverName  | awk -F "=" '{print $2}'`
CHADODB=`grep -v "#" $PROPDIR/modmine.properties.$REL | grep -m1 metadata.datasource.databaseName | awk -F "=" '{print $2}'`
fi

#***
LOG="$LOGDIR/$USER.$REL.$P"`date "+%y%m%d.%H%M"`  # timestamp of stag operations + error log

#SOURCES=cdna-clone,modmine-static,modencode-"$P"metadata
if [ -n "$P" ]
then
SOURCES=modmine-static,modencode-"$P"-metadata
elif [ -n "$L" ]
then
SOURCES=modmine-static
IFS=$','
for p in $L
do 
echo "---> $p"
SOURCES="$SOURCES",modencode-"$p"-metadata
done
IFS=$'\t\n'
else
#SOURCES=entrez-organism,modmine-static,modencode-metadata,fly-expression-score
SOURCES=modmine-static,modencode-metadata
#SOURCES=modencode-metadata,worm-network
fi



echo
echo "==================================="
echo "Building modmine-$REL on $MINEHOST."
echo "==================================="
echo "current directory: $MINEDIR"
echo "modencode data sources on: *** $DBHOST ***"
echo "Log: $LOG"
if [ "$FULL" = "n" ]
then
echo "Sources: $SOURCES"
fi
echo

if [ -n "$1" ]
then
SUB=$1
echo "Processing submission $SUB.."
fi

function setProjectFile {
#------------------------------------------------------------------------
# setting the appropriate project.xml
#
# necessary if we want to maintain generic chado db such as modchado-dev
# in addition to the project ones used for a full build
# e.g. modchado-piano
#
#------------------------------------------------------------------------
RETURNDIR=$PWD

if [ -n "$P" -o "$FULL" = "y" ]
then
return
fi

if [ "$META" = "y" -o "$VALIDATING" = "y" ]
then
cd $MINEDIR
if [ -n "$1" ]
then
echo "resetting project.xml.."
# resetting: going back to the normal situation
mv project.xml.original project.xml
else
# setting the dev project
echo "setting the project.xml file for a generic chado..."
cp -u project.xml project.xml.original # cp only if .original is not there
cp $SCRIPTDIR/project.xml .
fi
cd $RETURNDIR
fi

}

function interact {
# if testing, wait here before continuing
if [ $INTERACT = "y" -o $STOP = "y" ]
then
echo "$1"
echo "Press return to continue (^C to exit).."
echo -n "->"
read
fi

}

function initChado {
# rebuild skeleton chado db
# if a parameter is passed, this is the project name and the function
# will build the specific modchado
#
# e.g. initChado celniker
# will build modchado-celniker
#

if [ -n "$1" ]
then
CHADODB="modchado-$1"
fi

RETURNDIR=$PWD

# do a back-up?
if [ "$BUP" = "y" ]
then
dropdb -e "$CHADODB"-old -h $DBHOST -U $DBUSER;
createdb -e "$CHADODB"-old -T $CHADODB -h $DBHOST -U $DBUSER\
|| { printf "%b" "\nMine building FAILED. Please check previous error message.\n\n" ; exit 1 ; }
fi

if [ "$CHADOAPPEND" = "n" ]
then 
dropdb -e "$CHADODB" -h "$DBHOST" -U "$DBUSER";
createdb -e $CHADODB -h $DBHOST -U $DBUSER || { printf "%b" "\nMine building FAILED. Please check previous error message.\n\n" ; exit 1 ; }
# initialise it
cd $MINEDIR
psql -q -o /dev/null -d  $CHADODB -h $DBHOST -U $DBUSER < $SCRIPTDIR/build_empty_chado.sql\
|| { printf "%b" "\nMine building FAILED. Please check previous error message.\n\n" ; exit 1 ; }


if [ "$CHADODB" = "modchado-white" ]
then
echo
echo "WARNING: dropping constraint attribute_name_key in modchado-white and substituting with a unique index for record with char_length(value) < 2700.."
# there are white submissions that give an error in the row size of the index because of the very
# long string in the value of the attribute (a seq). Here postgres suggestions.
# ERROR:  index row size 3376 exceeds btree maximum, 2712
# HINT:  Values larger than 1/3 of a buffer page cannot be indexed.
# Consider a function index of an MD5 hash of the value, or use full text indexing
psql -h $DBHOST -d $CHADODB -U $DBUSER -c "alter table attribute drop constraint attribute_name_key;"
psql -h $DBHOST -d $CHADODB -U $DBUSER -c "CREATE UNIQUE INDEX att_value_idx ON  attribute (name, heading, rank, value, type_id)
    where char_length(value) < 2700;"
fi

fi

cd $RETURNDIR
}

function runTest {
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
mail -s "$1 report, also in file://$REPORTS/$1.html" $RECIPIENTS < $REPORTS/$1.html
#elinks $REPORTS/$1.html
echo
echo "acceptance test results in "
echo "$REPORTS/$1.html"
echo
}

function filer {
# in a directory, look for all the .chadoxml files that are compressed
# i.e. newly downloaded. Depending on their location, they will be
# decompressed and moved in the
# proper directory (new|update), which is the argument of the function
# also put a de-wiggled file in the load directory, ready to load into chado
for sub in *.chadoxml
do
  file $sub | grep compressed > bintest
  if [ -s bintest ]
    then
    # unzip and rename dowloaded file
    DCCID=`echo $sub | cut -f 1 -d.`
    echo "unzipping $1 file $DCCID"
    echo "unzipping $1 file $DCCID" | cat >> $LOGDIR/downloaded.log$WLOGDATE
         
    gzip -S .chadoxml -d $sub
    mv $DCCID $MIRROR/$1/$sub
  	FOUND=y
    
    if [ "$sub" = "2745.chadoxml" ]
    then
    changeFeatType $MIRROR/$1/$sub > $LOADDIR/$sub
    else
    dewiggle $MIRROR/$1/$sub > $LOADDIR/$sub
    fi

    if [ -n "$2" ]
    then
     rm $MIRROR/new/$sub
     cp -s $MIRROR/update/$sub $MIRROR/new
    fi

  fi
done
}

function validate {
# validate sub $sub from directory $dir
sub=$1
dir=$2
RETURNDIR=$PWD


cd $MINEDIR
echo "Building modMine $REL"
echo
# new build. static, metadata
../bio/scripts/project_build -a $SOURCES -V $REL $V -b localhost /tmp/mod-meta\
|| { printf "%b" "\n modMine build FAILED.\n" ; exit 1 ; }
cd postprocess
ant -v -Daction=set-missing-chromosome-locations -Drelease=$REL\
|| { printf "%b" "\n modMine build (only metadata) FAILED while setting locations.\n" ; exit 1 ; }

cd $MINEDIR
# save factor file and run acceptance tests
NAMESTAMP=`echo $sub | cut -d. -f1`
cp $MINEDIR/integrate/all_subs_report.csv $REPORTS/expFactor/$NAMESTAMP.csv

runTest $NAMESTAMP

# go back to the reference chado directory and mv chado file in 'done'
# this is to allow to run the validation as a cronjob
cd $MIRROR/new
rm -f ./$sub
if [ "$dir" = "new" ]
then
cp -s ./validated/$sub .
else
cp -s ../$1/validated/$sub .
fi
 

cd $RETURNDIR
}

function fillChado {
# fillChado full_path_to_chadoxml
# e.g. fillChado /shared/data/modmine/subs/chado/load/100.chadoxml
# 
# NB: path is assumed to be fix. 
#
# run stag and add the dccid

DCCID=`echo $1 | cut -f 8 -d/ |cut -f 1 -d.`
EDATE=`grep -w ^$DCCID $DATADIR/ftplist | grep -v true | sed -n 's/.*\t//;p'`

# check if the sub is already in chado: in case skip!
ISIN=`psql -h $DBHOST -d $CHADODB -U $DBUSER -q -t -c "select experiment_id from experiment_prop where name = 'dcc_id' and  value='$DCCID';"`
if [ -n "$ISIN" ]
then
echo "Submission $DCCID is already in chado: skipping it.."
echo "`date "+%y%m%d.%H%M"` $DCCID already loaded, skipping it.." >> $LOG
else

echo -n "filling $CHADODB db with $DCCID (eDate: $EDATE) -- "
date "+%d%b%Y %H:%M"
echo >> $LOG
echo -n "`date "+%y%m%d.%H%M"`  $DCCID " >> $LOG

## we should test more the use with this option (according to profiler is cheaper)
#stag-storenode.pl -D "Pg:$CHADODB@$DBHOST" -user $DBUSER -password \
#$DBPW -noupdate cvterm,dbxref,db,cv,feature -cache feature=1 $1 

stag-storenode.pl -D "Pg:$CHADODB@$DBHOST" -user $DBUSER -password \
$DBPW -noupdate cvterm,dbxref,db,cv,feature $1

exitstatus=$?

if [ "$exitstatus" = "0" ]
then 
psql -h $DBHOST -d $CHADODB -U $DBUSER -c "insert into experiment_prop (experiment_id, name, value, type_id) select max(experiment_id), 'dcc_id', '$DCCID', 1292 from experiment_prop;"

# insertion of embargo date: temporary until all subs have it
DBDATE=`psql -h $DBHOST -d $CHADODB -U $DBUSER -q -t -c "select value from experiment_prop where name = 'Embargo Date' and  experiment_id=(select max(experiment_id) from experiment_prop);"`

if [ -z "$DBDATE" ]
then
echo "Adding Embargo Date: $EDATE.."
CVDATE=`psql -h $DBHOST -d $CHADODB -U $DBUSER -q -t -c "select cvterm_id from cvterm where name = 'date';"`

psql -h $DBHOST -d $CHADODB -U $DBUSER -c "insert into experiment_prop (experiment_id, name, value, type_id) select max(experiment_id), 'Embargo Date', '$EDATE', $CVDATE from experiment_prop;"

echo -n "  added embargo date: $EDATE " >> $LOG
fi

if [ -e "$PATCHDIR/applied_patches_$DCCID.chadoxml" ]
then
echo "$DCCID: adding patch file."
stag-storenode.pl -D "Pg:$CHADODB@$DBHOST" -user $DBUSER -password \
$DBPW -noupdate cvterm,dbxref,db,cv,feature $PATCHDIR/applied_patches_$DCCID.chadoxml

exitstatus=$?
if [ "$exitstatus" != "0" ]
then 
echo -n "  ** ERROR loading patch file **" >> $LOG
fi
else
echo -n "  no patch file " >> $LOG
echo "$DCCID: no patch file."
fi

else
echo
echo -n "  ERROR: stag-storenode FAILED." >> $LOG
echo "$DCCID  stag-storenode FAILED. SKIPPING SUBMISSION."
echo
STAGFAIL=y
fi

fi
}

function processOneChadoSub {
# processOneChadoSub {new|update}
cd $MIRROR/$1

# if it is a symbolic link and this is not the given input
# we skip that file
if [ -L "$sub" -a "$LOOPVAR" = "*.chadoxml" ]
then
 continue
fi
echo
echo "================"
echo "$sub..."
echo "================"

#
# for validation, we rebuild chado for each file
#
if [ "$VALIDATING" = "y" ]
then
initChado
fi

fillChado $LOADDIR/$sub

# if stag failed, we set aside the sub
if [ "$STAGFAIL" = "y" ]
then
STAGFAIL=n
mv $sub $MIRROR/$1/failed
continue
fi

# if building the release, we move the file
# if [ "$FULL" = "y" ]
# then
# mv $sub $MIRROR
# ln -s ../$sub $sub
# fi

#if we are validating, we'll process an entry at a time
if [ "$VALIDATING" = "y" ]
then

validate $sub $1

fi 

}

function prepareForFull { #this can probably go
#------------------------------------------------------
# prepare directories for stag in case of FULL release
#------------------------------------------------------
# TODO: if no $1, no infile, no incr -> do all
# REWRITE

echo "Function prepareForFull needs rewriting!!"
exit;

# cd $DATADIR
# mv *.chadoxml $DATADIR/new
# mv $DATADIR/update/validated/*.chadoxml $DATADIR/new
# mv $DATADIR/new/validated/*.chadoxml $DATADIR/new
# cd $DATADIR/new
# for sub in *.chadoxml
#  do
#  # if found symbolic link not to err directory throw an error
#  if [ -L "$sub" -a ! -e "$DATADIR/new/err/$sub" ]
#    then
#      echo "WARNING: $sub is missing from load directory" | tee -a $LOG
# 	   STOP=y # TODO: if incr you should exit! or exit always
#    fi
# # CHECK XML and experiment title
# if [ ! -L "$sub" ]
# then
# echo $sub
# xmllint -noout $sub
# grep -2 "<experiment id"  $sub | grep "ename></uniquen"
# fi
# 
#  done

# TODO: add check deletions 

}

function dewiggle {
# dewiggle dccid
# e.g. dewiggle 100
# to remove wiggle data from the chadoxml file
# put the slimmed file in load directory
sed '/<wiggle_data id/,/<\/wiggle_data>/d' $1 | sed '/<data_wiggle_data/,/<\/data_wiggle_data>/d'
}

function changeFeatType {
# needed to deal with piano 2745 (mRNA->transcript)    
sed 's/<name>mRNA</<name>transcript</g' $1 | sed 's/<accession>0000234</<accession>0000673</g'   
}


function doProjectList {
#--------------------------------------------------
# building the list of live dccid for each project 
#--------------------------------------------------
# TODO: get the project name from a list..

#grep released ftplist | grep false | awk '{print $1, $(NF-2)}' | tr -d ,

grep released $DATADIR/ftplist | grep false | grep -vw true | grep -i celniker, | awk '{print $1}' > $DATADIR/celniker.live
grep released $DATADIR/ftplist | grep false | grep -vw true | grep -i henikoff, | awk '{print $1}' > $DATADIR/henikoff.live
grep released $DATADIR/ftplist | grep false | grep -vw true | grep -i karpen, | awk '{print $1}' > $DATADIR/karpen.live
grep released $DATADIR/ftplist | grep false | grep -vw true | grep -i lai, | awk '{print $1}' > $DATADIR/lai.live

grep released $DATADIR/ftplist | grep false | grep -vw true | grep -i lieb, | awk '{print $1}' > $DATADIR/lieb.live
grep released $DATADIR/ftplist | grep false | grep -vw true | grep -i macalpine, | awk '{print $1}' > $DATADIR/macalpine.live
grep released $DATADIR/ftplist | grep false | grep -vw true | grep -i piano, | awk '{print $1}' > $DATADIR/piano.live
grep released $DATADIR/ftplist | grep false | grep -vw true | grep -i snyder, | awk '{print $1}' > $DATADIR/snyder.live
grep released $DATADIR/ftplist | grep false | grep -vw true | grep -i waterston, | awk '{print $1}' > $DATADIR/waterston.live
grep released $DATADIR/ftplist | grep false | grep -vw true | grep -i white, | awk '{print $1}' > $DATADIR/white.live
grep released $DATADIR/ftplist | grep false | grep -vw true | grep -i oliver, | awk '{print $1}' > $DATADIR/oliver.live

cat $DATADIR/celniker.live $DATADIR/lai.live > $DATADIR/celnikerlai.live
cat $DATADIR/waterston.live $DATADIR/piano.live > $DATADIR/waterstonpiano.live

}

function getFiles {
#---------------------------------------
# getting the chadoxml from ftp site 
#---------------------------------------

# this for confirmation the program runs and to avoid to grep on a non-existent file
touch $LOG

WLOGDATE=
if [ "$FULL" = "y" ]
then
# we want to keep a copy of the wget log
WLOGDATE=`date "+%y%m%d.%H%M"`
fi
# otherwise a log wget.log is kept (until next wget). 

echo
echo "Getting data from $FTPURL. Log in $LOGDIR/wget.log$WLOGDATE"
echo


#FTPURL=http://submit.modencode.org/submit/public/
# NB: files are dowloaded gzipped and later decompressed
#
#  a append to log
# -N timestamping
# -t number of tries

if [ -n "$SUB" ]
then
# doing only 1 sub
LOOPVAR="$SUB"
elif [ -n "$INFILE" ]
then
# use the list provided in a file
LOOPVAR=`cat $INFILE`
else
# get the full list from the ftp site and save it for reference
wget -O - $FTPURL/list.txt | sort > $FTPARK/`date "+%y%m%d"`.list
rm $DATADIR/ftplist
ln -s $FTPARK/`date "+%y%m%d"`.list $DATADIR/ftplist

# get the list of live dccid and use it as loop variable
grep released $DATADIR/ftplist | grep false | grep -vw true | awk '{print $1}' > $DATADIR/all.live
LOOPVAR=`cat $DATADIR/all.live`
doProjectList

# get also the list of deprecated entries with their replacement
# this is now obsolete, still useful for doc?
grep released $DATADIR/ftplist | grep true | awk '$2 == "true" {print $1, " -> ", $3 }' > $DATADIR/deprecation.table
# true of superseded can be on 3 or 4 position, and the superseding sub in 4 or 5
grep released $DATADIR/ftplist | grep true | awk '$3 == "true" {print $1, " -> ", $4 }' > $DATADIR/superseded.table
grep released $DATADIR/ftplist | grep true | awk '$4 == "true" {print $1, " -> ", $5 }' >> $DATADIR/superseded.table
awk '{print $1}' $DATADIR/deprecation.table > $DATADIR/all.dead
awk '{print $1}' $DATADIR/superseded.table >> $DATADIR/all.dead

# do the deprecations file
grep released $DATADIR/ftplist | grep true | awk '$2 == "true" {print $3","$1 }' | grep -v unknown > $DATADIR/depr
grep released $DATADIR/ftplist | grep true | awk '$3 == "true" {print $4","$1 }' >> $DATADIR/depr
grep released $DATADIR/ftplist | grep true | awk '$4 == "true" {print $5","$1 }' >> $DATADIR/depr

mv $DATADIR/deprecations $FTPARK/dep.`date "+%y%m%d"`
sort -u $DATADIR/depr > $DATADIR/deprecations

fi

cd $MIRROR/new

interact "START WGET NOW"

for sub in $LOOPVAR
do

 wget -t3 -N --header="accept-encoding: gzip" $FTPURL/get_file/$sub/extracted/$sub.chadoxml  --progress=dot:mega 2>&1 | tee -a $LOGDIR/wget.log$WLOGDATE

cd $PATCHDIR
 wget -t3 -N $FTPURL/get_file/$sub/extracted/applied_patches_$sub.chadoxml --progress=dot:mega 2>&1 | tee -a $LOGDIR/wget.log$WLOGDATE
# it gets the html if nothing there
rm -vf *extracted*
# new style: if nothinbg there the file is an html file
FILETYPE=`file -b applied_patches_$sub.chadoxml | awk '{print $1}'`
if [ "$FILETYPE" = "HTML" ]
then
rm applied_patches_$sub.chadoxml
fi
cd $MIRROR/new

done

}

function loadChadoSubs {
echo "STARTING STAG $1 ------------------>" >> $LOG
if [ -n "$SUB" ]
then
LOOPVAR="$SUB.chadoxml"
initChado
elif [ -n "$INFILE" ]
then
# use the list provided in a file
LOOPVAR=`sed 's/$/.chadoxml/g' $INFILE | cat`
echo "********"
echo $LOOPVAR
initChado

elif [ -n "$1" ]
then
# this is a project name, load it
LOOPVAR=`sed 's/$/.chadoxml/g' $DATADIR/$1.live | cat`
initChado $1
else
LOOPVAR="*.chadoxml"
initChado
fi

if [ -n "$SUB" ]
then
# doing one sub only, using loop because so expects processOneChadoSub
for sub in $LOOPVAR
do
processOneChadoSub new
done

elif [ "$VALIDATING" = "y" -a -z "$INFILE" ]
then
# validating all: is the configuration used by cronmine.
# run processOneChadoSub both in new and update directories

echo "====================="
echo "validating new..."
echo "====================="
for sub in $LOOPVAR
do
processOneChadoSub new
done

echo "====================="
echo "validating update..."
echo "====================="
for sub in $LOOPVAR
do
processOneChadoSub update
done

else
# when not validating or using given (list of) sub(s)
for sub in $LOOPVAR
do
processOneChadoSub new
done

fi

# if we are validating, that's all
if [ "$VALIDATING" = "y" ]
then
echo
echo "**VALIDATION FINISHED**"
echo
exit;
fi

echo -n "$sub loaded in chado -- "
date "+%d%b%Y %H:%M"


}

interact

########################################
#
# MAIN
#
########################################


#---------------------------------------
# get the xml files
#---------------------------------------
#
if [ "$WGET" = "y" ] # new fz checkFtp ?
then

getFiles

#-------------------------------------------------
# check if any NEW file, decompress and rename it
#-------------------------------------------------
# arg: the destination directory
cd $MIRROR/new
filer new

#-------------------------------------------------
# check if any UPDATED file
#-------------------------------------------------
# arg2: flag to make change the link in the
# reference directory (new)
# in this case it must remove link to .. and substitute
# with ../update
cd $MIRROR
filer update changelink

#-------------------------------------------------
# check if any UPDATED file from previous updates
#-------------------------------------------------
cd $MIRROR/update
filer update

#-------------------------------------------------
# check 'validated' directories in new and update:
# 
#-------------------------------------------------
cd $MIRROR/new/validated
filer new

cd $MIRROR/update/validated
filer update changelink
# link from ../update/validated to ../update

#------------------------------------------------------------------------
# check if any update in the ERR directory, decompress, mv and rename it
#------------------------------------------------------------------------
cd $MIRROR/new/err
filer new

if [ "$FOUND" = "n" ]
then
	echo
	echo "no new data found on ftp. exiting."
	echo
	exit 0;
fi

interact

fi #if $WGET=y

#----------------------------------------------
# set project.xml file (for full build or dev)
#----------------------------------------------
setProjectFile

#------------------------------------------
# fill chado db (and validate if required)
#------------------------------------------

# if [ "$FULL" = "y" -a "$PREP4FULL" = "y" ]
# then
# prepareForFull
# 
# interact "just finished: directories preparation"
# 
# cd $MINEDIR
# $SCRIPTDIR/checkdel.sh
# 
# fi


if [ "$STAG" = "y" ]
then

if [ "$FULL" = "y" ]
then
interact "Reloading all chadoes. All existing databases in $DBHOST will be rebuilt."
#loadChadoSubs lai
#loadChadoSubs piano
loadChadoSubs henikoff
loadChadoSubs lieb
loadChadoSubs oliver
loadChadoSubs macalpine
loadChadoSubs snyder
loadChadoSubs karpen
loadChadoSubs white
loadChadoSubs celnikerlai
loadChadoSubs waterstonpiano

interact "Updating chado DBs: adding deletion flag and deprecations."

$SCRIPTDIR/flag_deleted.sh
$SCRIPTDIR/add_deprecations.sh

elif [ -n "$P" ]
then
loadChadoSubs $P
elif [ -n "$L" ]
then
IFS=$','
for p in $L
do 
echo "---> $p"
IFS=$'\t\n'
loadChadoSubs $p
IFS=$','
echo " " >> $LOG
echo " " >> $LOG
done
IFS=$'\t\n'
else
loadChadoSubs
fi

interact
else
echo "Using previously loaded chado..."
fi # if $STAG=y


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
echo; echo "Restarting build using last available back-up db.."
../bio/scripts/project_build -V $REL $V -l localhost $ARKDIR/build/mod-final.dmp\
|| { printf "%b" "\n modMine build (restart) FAILED.\n" ; exit 1 ; }
elif [ $QRESTART = "y" ]
then
# restart build without recovering last dumped db
echo; echo "Quick restart of the build (using current db).."
../bio/scripts/project_build -V $REL $V -r localhost $ARKDIR/build/mod-final.dmp\
|| { printf "%b" "\n modMine build (quick restart) FAILED.\n" ; exit 1 ; }
elif [ $META = "y" ]
then
# new build. static, metadata, organism
echo "SOURCES: $SOURCES"
../bio/scripts/project_build -a $SOURCES -V $REL $V -b localhost /tmp/mod-meta\
|| { printf "%b" "\n modMine build (only metadata) FAILED.\n" ; exit 1 ; }
cd postprocess
ant -v -Daction=set-missing-chromosome-locations -Drelease=$REL\
|| { printf "%b" "\n modMine build (only metadata) FAILED while setting locations.\n" ; exit 1 ; }
ant -v -Daction=modmine-metadata-cache -Drelease=$REL\
|| { printf "%b" "\n modMine build (only metadata) FAILED while building cache.\n" ; exit 1 ; }
else
# new build, all the sources
# get the most up to date sources ..
if [ $PREP4FULL = "y" ]
then
cd ../bio/scripts
./get_all_modmine.sh|| { printf "%b" "\n modMine build (get_all_modmine.sh) FAILED.\n" ; exit 1 ; }
fi
# .. and build modmine
cd $MINEDIR
../bio/scripts/project_build -V $REL $V -b localhost $ARKDIR/build/mod-final.dmp\
|| { printf "%b" "\n modMine build FAILED.\n" ; exit 1 ; }
fi

else
echo
echo "Using previously built modMine."
echo
fi #BUILD=y

#----------------------------------------------
# set project.xml back to the original state
#----------------------------------------------
setProjectFile back

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
if [ "$TEST" = "y" ] && [ "$VALIDATING" = "n" ]
then
if [ -n "$P" ]
then
NAMESTAMP="$P"_`date "+%y%m%d"`
else
NAMESTAMP="$REL"_`date "+%y%m%d.%H%M"`
fi
runTest $NAMESTAMP
fi

