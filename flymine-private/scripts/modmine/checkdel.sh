#!/bin/bash
#
# default usage: checkdel.sh rel
#
# sc 08/09
#
# NB: comm wants alphabetical sorting!
#
# TODO: merge with check_deprecation
#
DATADIR=/shared/data/modmine/subs/chado
VALDIR=$DATADIR/new/validated
CHECKDIR=$VALDIR/deprecationCheck
NEXTDIR=$DATADIR/new/load_in_next_full_release
DONTDIR=$DATADIR/deprecated

if [ ! -s "$DATADIR/dead.dccid" ]
then
echo
echo "List of deprecated dccid missing!! Please check and run again."
echo

exit
fi


function mv_deleted {
cd $1
echo "** in $1 .."
ls -1 *.chadoxml | sed 's/.chadoxml.*//g' | sort > $CHECKDIR/candidates
comm -12 $CHECKDIR/candidates $CHECKDIR/deleted > $CHECKDIR/toremove

LOOPVAR=`sed 's/$/.chadoxml/g' $CHECKDIR/toremove | cat`

for sub in $LOOPVAR
do
# mv real files, rm links
echo -n $sub
if [ -L "$sub" ]
then
echo -n -
rm $sub
else
echo -n +
mv $sub $DONTDIR
fi
done

}

cd $DATADIR
sort $DATADIR/dead.dccid > $CHECKDIR/deleted

mv_deleted $DATADIR
mv_deleted $VALDIR
mv_deleted $NEXTDIR
mv_deleted $DATADIR/new
mv_deleted $DATADIR/update
mv_deleted $DATADIR/new/err
mv_deleted $DATADIR/new/genemodel
#mv_deleted $DATADIR/new/lai


echo
echo
echo "Directories now clean."
echo

#exit;

if [ -n "$1" ]
then
echo "Checking deprecations for incremental release..."

# get subs currently in release
elinks -dump 1 -dump-width 1  "http://intermine.modencode.org/query/service/template/results?name=getSubmissions&constraint1=Submission.title&op1=eq&value1=*&size=1000&format=tab" | tr -d [:blank:] | sort > $CHECKDIR/inrel


cd $VALDIR
ls -1 *.chadoxml | sed 's/.chadoxml.*//g' | sort > $CHECKDIR/rel_candidates

cd $CHECKDIR
awk '{print $3}' $DATADIR/deprecation.table | sort > updated
awk '{print $1}' $DATADIR/deprecation.table | sort > deprecated

comm -12 inrel $DATADIR/dead.dccid > depr.inrel

grep -wf depr.inrel $DATADIR/deprecation.table | awk '{print $3}' | sort > no1

comm -12 updated deprecated > updead
# we need to add those to catch the ones that are updating submissions updating other
# submissions already in the release.
# we could simply use the 'updated' file, but we will miss the ones that don't have an ancestor
# in the release 
grep -wf updead $DATADIR/deprecation.table | awk '{print $3}' | sort > no2

cat no1 no2 | sort -u > dont_load.list

count=`grep -c . dont_load.list`

echo
echo "DO NOT LOAD the following $count submissions in this incremental release:"
echo "(updating submissions in the current release)"
echo "------------------------------------------------------------"



# echo
# echo "List of submissions NOT to load in this incremental release:"
# echo "(updating submissions already in the current release)"
# echo "------------------------------------------------------------"
more -5 dont_load.list 

fi

