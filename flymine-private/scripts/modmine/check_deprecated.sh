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
# NB: comm wants alphabetical sorting!
#
DATADIR=/shared/data/modmine/subs/chado
VALDIR=$DATADIR/new/validated
CHECKDIR=$VALDIR/deprecationCheck
NEXTDIR=$DATADIR/load_in_next_full_release
DONTDIR=$DATADIR/neverIn

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
ls -1 *.chadoxml | sed 's/.chadoxml.*//g' | sort > $CHECKDIR/rel_candidates

cd $CHECKDIR
awk '{print $3}' $DATADIR/deprecated.dccid | sort > updated
awk '{print $1}' $DATADIR/deprecated.dccid | sort > deprecated

comm -12 updated deprecated > updead

sort sub.r$1 > in.now

comm -12 updated in.now > up.inrel
comm -12 deprecated in.now > depr.inrel

comm -23 rel_candidates $DATADIR/live.dccid > deleted.list
comm -12 rel_candidates $DATADIR/live.dccid > live_rel_candidates

grep -wf depr.inrel $DATADIR/deprecated.dccid | awk '{print $3}' | sort > dont_load.list
#grep -wf deprecated $DATADIR/deprecated.dccid | awk '{print $3}' | sort > dont_load.list

# we need to add those to catch the ones that are updating submissions updating other
# submissions already in the release.
# we could simply use the 'updated' file, but we will miss the ones that don't have an ancestor
# in the release 
grep -wf updead $DATADIR/deprecated.dccid | awk '{print $3}' | sort >> dont_load.list

LOOPVAR=`sed 's/$/.chadoxml/g' $CHECKDIR/deleted.list | cat`
echo "********"
echo "DELETED.."
echo $LOOPVAR
cd $VALDIR

for sub in $LOOPVAR
do
mv $sub $DONTDIR
done


LOOPVAR=`sed 's/$/.chadoxml/g' $CHECKDIR/dont_load.list | cat`
echo "********"
echo "FOR NEXT REL.."
echo $LOOPVAR

cd $VALDIR

for sub in $LOOPVAR
do
mv  $sub $NEXTDIR
done

cd $NEXTDIR
NN=`ls -1 *.chadoxml | grep -c .`

cd $VALDIR
IN=`ls -1 *.chadoxml | grep -c .`

cd $DONTDIR
DN=`ls -1 *.chadoxml | grep -c .`


echo
echo "There are $IN candidates for the incremental release, $NN submissions are postponed to the next full release"
echo "$DN submissions were already deleted before entering modMine" 
echo

exit;

# TODO, these don't work

elinks http://intermine.modencode.org/query/service/template/results?name=getSubmissions&constraint1=Submission.title&op1=eq&value1=*&size=1000&format=tab

wget "http://intermine.modencode.org/query/service/template/results?name=getSubmissions&constraint1=Submission.title&op1=eq&value1=*&size=1000&format=tab"

