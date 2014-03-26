#!/bin/bash
#
# default usage: automine.sh
#
# note: you should put the db password in ~/.pgpass if don't
#       want to be prompted for it
#
# sc

SUBDIR=/micklem/data/modmine/subs
DATADIR=$SUBDIR/chado

MIRROR=$DATADIR/mirror 
LOADDIR=$DATADIR/load
LOGDIR=$DATADIR/logs
FTPARK=$DATADIR/ark

PATCHDIR=$LOADDIR/patches
PREFIX="applied_patches_"

FTPURL=http://submit.modencode.org/submit/public
SUBDIR=/shared/data/modmine/subs
SCRIPTDIR=../bio/scripts/flymine/modmine/

RECIPIENTS=contrino@flymine.org,rns@flymine.org

# set minedir and check that modmine in path
MINEDIR=$PWD
BUILDDIR=$MINEDIR/integrate/build



# default settings: edit with care
WEBAPP=y         # build a webapp
ONLYPATCHES=n    # stag only patches
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
  -P project_name: as -M, but restricted to a project.
  -T list of projects: as -M, but using a (comma separated) list of projects.
	-f file_name: using a given list of submissions
	-v: verbode mode


Parameters: you can process
            a single submission                   (e.g. automine.sh 204 )
						a list of submission in an input file (e.g. automine.sh -V -f infile )
            all the available submissions         (e.g. automine.sh -F )

            you can also pass the release, overwriting the default
                                                  (e.g. automine.sh -F -r test )


EOF
	exit 0
}

echo

while getopts ":FP:T:f:iv" opt; do
	case $opt in

	F )  echo "- Full modMine realease"; FULL=y; INCR=n; REL=build;;
	P )  P=$OPTARG;echo "- Test build (metadata only) with project $P"; META=y; INCR=n; P="`echo $P|tr '[A-Z]' '[a-z]'`";;
	T )  PLIST=$OPTARG;echo "- Test build (metadata only) with projects $PLIST"; META=y; INCR=n; P="`echo $P|tr '[A-Z]' '[a-z]'`";;
	f )  INFILE=$OPTARG; echo "- Using given list of chadoxml files: "; SHOW="`cat $INFILE|tr '[\n]' '[,]'`"; echo $SHOW;;
	i )  echo "- Interactive mode" ; INTERACT=y;;
	v )  echo "- Verbose mode" ; V=-v;;
	h )  usage ;;
	\?)  usage ;;
	esac
done

shift $(($OPTIND - 1))

LOG="$LOGDIR/wrongPatches."$P`date "+%y%m%d"`  # timestamp of stag operations + error log

EDLIST=


echo "==================================="
echo "Checking files in $PATCHDIR"
echo "==================================="
echo "current directory: $MINEDIR"
echo "Log: $LOG"
echo
echo "NOTE: stag can deal with &amp; instead of &, and untrimmed spaces are not relevant"
echo


if [ -n "$1" ]
then
SUB=$1
echo "Processing submission $SUB.."
fi


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




function check_patch {
echo "STARTING check $1 ------------------>" 
if [ -n "$SUB" ]
then
LOOPVAR="$SUB.chadoxml"
elif [ -n "$INFILE" ]
then
# use the list provided in a file
LOOPVAR=`sed 's/$/.chadoxml/g' $INFILE | cat`
echo "********"
echo $LOOPVAR

elif [ -n "$1" ]
then
# this is a project name, load it
LOOPVAR=`sed 's/$/.chadoxml/g' $DATADIR/$1.live | cat`
else
LOOPVAR="*.chadoxml"
fi

if [ -n "$SUB" ]
then
# doing one sub only, using loop because so expects processOneChadoSub
for sub in $LOOPVAR
do
check_one
done

else
# when not validating or using given (list of) sub(s)
for sub in $LOOPVAR
do
check_one
done

fi

}

function add_to_list {
DCCID=`echo $1 | cut -f 1 -d.`

    echo "$DCCID" | cat >> $LOGDIR/downloaded.log$WLOGDATE

}


function check_one {

# if there is no patch file we skip the check
if [ -s "$PATCHDIR/$PREFIX$sub" ]
 then

#echo
#echo "================"
echo -n "$sub..."

grep "<uniquename>" $PATCHDIR/$PREFIX$sub | tr -s ' ' | sort -u > p
grep "<uniquename>" $LOADDIR/$sub | tr -s ' ' | tail -1 > s

echo `diff p s`

diff p s > $sub.diff

if [ -s "$sub.diff" ]
then
echo "$sub..." | cat >> $LOG
cat $sub.diff >> $LOG
echo "-------------" | cat >> $LOG

EDLIST="$EDLIST $PREFIX$sub"
fi
 
rm $sub.diff p s
 
 else
 echo "No patch file for $PATCHDIR/$PREFIX$sub"
 fi

}


interact

########################################
#
# MAIN
#
########################################



if [ "$FULL" = "y" ]
then
check_patch oliver
check_patch celniker
check_patch henikoff
check_patch karpen
check_patch lai
check_patch lieb
check_patch macalpine
check_patch piano
check_patch snyder
check_patch waterston
check_patch white
elif [ -n "$P" ]
then
check_patch $P
else
check_patch
fi

if [ -n "$EDLIST" ]
then 
cd $PATCHDIR
/usr/bin/nedit $EDLIST &

echo "==========================="
echo "     $P                    "
echo $EDLIST

else
echo "************************"
echo "$P All fine!"
echo "************************"
fi
 
