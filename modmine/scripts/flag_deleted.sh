#!/bin/bash
#
# default usage: flag_deleted.sh
#             or flag_deletd.sh -P project
#
# sc

DATADIR=/micklem/data/modmine/subs/chado
LOGDIR=$DATADIR/logs

INTERACT=y
DOIT=y
INFILE=$DATADIR/all.dead

DBHOST=modfast
DBUSER=modmine

#PRO="henikoff"
PRO="lieb henikoff macalpine oliver snyder karpen white celnikerlai waterstonpiano"

progname=$0

umask 0002

function usage () {
	cat <<EOF

Usage:
$progname [-b] 
	-b: batch mode (all operations are executed without warning)
    -m: using the specified host (default: modfast)
    -P: using a SINGLE specified project
    -f: using a file with deleted submission id

examples:

$progname
$progname -b idem, batch mode (careful!)
$progname -m modprod1 using dbhost modprod1
$progname -P waterstonpiano -f remove_these 

EOF
	exit 0
}

echo

while getopts ":bm:P:f:" opt; do
	case $opt in
	b )  echo "- BATCH mode" ; INTERACT=n;;
	m )  DBHOST=$OPTARG; echo "- Using db host $DBHOST";;
    P )  PRO=$OPTARG; echo "- Using SINGLE project $PRO";;
    f )  INFILE=$OPTARG; echo "- Using file with deleted id $f";;
	h )  usage ;;
	\?)  usage ;;
	esac
done

shift $(($OPTIND - 1))



LOG="$LOGDIR/flag_deletions."`date "+%y%m%d.%H%M"`


function interact {
if [ "$INTERACT" = "y" ]
then
echo "** $1"
echo "Press return to continue or [n] to skip this step (^C to exit).."
echo -n "->"
read DOIT
fi

}



function flag {
RETURNDIR=$PWD
cd $DATADIR
LOOPVAR=`cat $INFILE`

for p in $PRO
do
CHADODB="modchado-$p"

echo
echo "====== $p.."
echo "$p project" >> $LOG
echo >> $LOG

TYPEID=`psql -h $DBHOST -d $CHADODB -U $DBUSER -q -t -c "select cvterm_id from cvterm where name = 'string';"`

for DCCID in $LOOPVAR
do
#echo $DCCID
ISIN=`psql -h $DBHOST -d $CHADODB -U $DBUSER -q -t -c "select experiment_id from experiment_prop where name = 'dcc_id' and  value='$DCCID';"`
if [ -n "$ISIN" ]
then
# check if already deleted (add exp_id?)
ISDEL=`psql -h $DBHOST -d $CHADODB -U $DBUSER -q -t -c "select experiment_id from experiment_prop where name = 'deleted' and  value='$DCCID';"`
if [ -n "$ISDEL" ]
then
echo "Submission $DCCID is already deleted in $CHADODB.."
else
# if not, add deleted flag
echo "Submission $DCCID: flagging as deleted in $CHADODB.."
echo "$DCCID: flagged as deleted." >> $LOG
psql -h $DBHOST -d $CHADODB -U $DBUSER -c "insert into experiment_prop (experiment_id, name, value, type_id) values ('$ISIN', 'deleted', '$DCCID', $TYPEID);"
fi
fi

done

echo "=========================================" >>$LOG
done

cd $RETURNDIR
echo
}

########################################
#
# MAIN
#
########################################

echo "Using file: "
echo "`ls -oh $INFILE`"
echo "to flag deleted submission on $DBHOST for projects: $PRO"
echo 
interact "Flagging deleted subs"
if [ "$DOIT" != "n" ]
then
flag
fi


more $LOG

echo
echo "Log file available: $LOG"
echo "bye!"
