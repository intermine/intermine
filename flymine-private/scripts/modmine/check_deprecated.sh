#!/bin/bash
#
# default usage: check_deprecated.sh rel
#
# note: for the moment pass in the release
#
# sc 07/09
#
# TODO:- use webservice to get automatically the list of current subs
#        no need to pass the release then
#      - then: add to automine (?)

DATADIR=/shared/data/modmine/subs/chado
VALDIR=$DATADIR/new/validated
CHECKDIR=$VALDIR/deprecationCheck
NEXTDIR=$VALDIR/load_in_next_full_release

if [ -z "$1" ]
then
echo
echo "You need to specify the current modMine release number"
echo "Note that you also need to have built the file $CHECKDIR/sub.r{relNr}, "
echo "for example exporting it from modMine."
echo

exit

else
if [ ! -s "$CHECKDIR/sub.r$1" ]
then
echo
echo "File $CHECKDIR/sub.r$1 is missing."
echo "You need to build it with the list of submission currently in modMine, for example exporting a query result from modMine."
echo

exit
else
echo
echo "Running check of deprecated submissions from release $1 (we will not add their replacement submissions to an incremental release, where the old ones will be still present."
echo "...."
echo
fi

fi

cd $VALDIR
ls -1 *.chadoxml | sed 's/.chadoxml.*//g' | sort -n > $CHECKDIR/rel_candidates

cd $CHECKDIR
awk '{print $3}' $DATADIR/deprecated.dccid | sort -n > updated
awk '{print $1}' $DATADIR/deprecated.dccid | sort -n > deprecated

comm -12 deprecated sub.r$1 > depr.inrel
comm -12 updated sub.r$1 > up.inrel

grep -wf depr.inrel $DATADIR/deprecated.dccid | awk '{print $3}' | sort -n > dont_load.list

cat up.inrel >> dont_load.list

LOOPVAR=`sed 's/$/.chadoxml/g' dont_load.list | cat`
echo "********"
echo $LOOPVAR

cd $VALDIR

for sub in $LOOPVAR
do
mv $sub $NEXTDIR
done

exit;

# TODO, these don't work

elinks http://intermine.modencode.org/query/service/template/results?name=getSubmissions&constraint1=Submission.title&op1=eq&value1=*&size=1000&format=tab

wget "http://intermine.modencode.org/query/service/template/results?name=getSubmissions&constraint1=Submission.title&op1=eq&value1=*&size=1000&format=tab"

