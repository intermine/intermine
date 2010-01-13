#!/bin/bash
#
# default usage: checkdel.sh
#
# sc 08/09
#
# NB: comm wants alphabetical sorting!
#
# NB: it could be used to check deprecation for incremental release, to be tested
# usage: checkdel.sh rel
#
# TODO: check for files that are not the live list
#
DATADIR=/shared/data/modmine/subs/chado

MIRROR=$DATADIR/mirror
LOADDIR=$DATADIR/load

DONTDIR=$DATADIR/ade
CHECKDIR=$DONTDIR/deprecationCheck
NEXTDIR=$MIRROR/new/load_in_next_full_release

if [ ! -s "$DATADIR/all.dead" ]
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
echo
done

# looking for ghost subs
ls -1 *.chadoxml | sed 's/.chadoxml.*//g' | sort > $CHECKDIR/candidates
comm -23 $CHECKDIR/candidates $CHECKDIR/valid > $CHECKDIR/toremove

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
mv $sub $DONTDIR/neverIn
fi
echo
done

}

cd $DATADIR
sort $DATADIR/all.dead > $CHECKDIR/deleted
sort $DATADIR/all.live > $CHECKDIR/valid

mv_deleted $LOADDIR

mv_deleted $MIRROR
mv_deleted $NEXTDIR
mv_deleted $MIRROR/new
mv_deleted $MIRROR/update
mv_deleted $MIRROR/new/validated
mv_deleted $MIRROR/update/validated
mv_deleted $MIRROR/new/err

echo
echo
echo "Directories now clean."
echo
echo "Compressing disposed files.."

cd $DONTDIR
gzip *.chadoxml
cd $DONTDIR/neverIn
gzip *.chadoxml

echo "..done."

#exit;

# THE FOLLOWING NEEDS TEST

if [ -n "$1" ]
then
echo "Checking deprecations for incremental release..."

# get subs currently in release
elinks -dump 1 -dump-width 1  "http://intermine.modencode.org/query/service/template/results?name=getSubmissions&constraint1=Submission.title&op1=eq&value1=*&size=1000&format=tab" | tr -d [:blank:] | sort > $CHECKDIR/inrel


cd $MIRROR/new/validated
ls -1 *.chadoxml | sed 's/.chadoxml.*//g' | sort > $CHECKDIR/rel_candidates

cd $CHECKDIR
awk '{print $3}' $DATADIR/deprecation.table | sort > updated
awk '{print $1}' $DATADIR/deprecation.table | sort > deprecated

comm -12 inrel $DATADIR/all.dead > depr.inrel

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

