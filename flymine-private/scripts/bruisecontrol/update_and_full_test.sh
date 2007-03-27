#!/bin/sh -

RUNNING_FILE=~/public_html/tests/.running
TRUNK_DIR=~/public_html/tests/trunk
BUILD_PROJ=$TRUNK_DIR/intermine/all
ARCHIVED_DIR=~/public_html/tests/archived
TIME_STAMP=`date '+%y%m%d_%H%M'`
ARCHIVE_TO=$ARCHIVED_DIR/$TIME_STAMP
LATEST_DIR=$ARCHIVED_DIR/latest
STD_ERR=~/public_html/tests/stderr.log
JUNIT_FAIL_FILE=~/public_html/tests/previous_junit_failures
URL_PREFIX='http://bc.flymine.org'

. ~/.bashrc

# -------------------------------------------------------------------------- #
# Check whether we really need to do anything at all
# -------------------------------------------------------------------------- #

if [ -f "$RUNNING_FILE" ]; then
  echo `date`: Not starting tests because $RUNNING_FILE exists
  exit
fi

cd "$TRUNK_DIR"
svn status -u | grep '^.......\*'

if [ $? -eq 1 -a "$#" -eq "0" ]; then
  echo "*** $TIME_STAMP no updates in repository ***"
  exit
fi

# -------------------------------------------------------------------------- #
# Check the last update was at least 10 minutes ago
# -------------------------------------------------------------------------- #

LAST_CHANGE=$(svn log --revision HEAD | perl -ne 'print "$1" if /.*\|.*\| (.*) \|/')
LAST_CHANGE=`date -d "$LAST_CHANGE" +%s`
DIFF=$((`date +%s`-$LAST_CHANGE))
echo "$DIFF seconds since last change"

if [ $DIFF -lt $((60*10)) ]; then
  echo "`date`: Need to wait $((60*10-$DIFF)) more seconds"
  if [ "$#" -eq "0" ]; then
    exit
  fi
else
  echo "`date`: 10 minutes have pasted since last update - lets do it..."
  date
fi

touch "$RUNNING_FILE"

echo new run > $STD_ERR

# -------------------------------------------------------------------------- #
# Find out who to blame when things go wrong
# -------------------------------------------------------------------------- #

NEXT=$(($(svn info | grep "Revision" | cut -c 11-)+1))
BLAME=`svn log -r $NEXT:HEAD | grep '^r[0-9]' | awk '{print $3;}' | sort | uniq`
BLAME=`echo $BLAME | sed 's/ /@flymine.org, /g'`@flymine.org
echo "BLAME = $BLAME"

umask 0022
mkdir -p "$ARCHIVE_TO"
mkdir -p "$LATEST_DIR"

UPDATE=$(svn update svn://svn.flymine.org/flymine/trunk $TRUNK_DIR)

cd $BUILD_PROJ
ant clean-all 

if [ "$#" -eq "0" ]; then
  TARGET=fulltest
else
  TARGET=$1
fi

for i in bio-fulldata-test bio-test webservice-test unittest testmodel-webapp-userprofile testmodel-webapp notxmltest truncunittest fulldatatest flatmodetest genomictest
do
  dropdb $i
  createdb $i
done

cd ../../testmodel/dbmodel
ant build-db > $ARCHIVE_TO/ant_log.txt 2>> $STD_ERR


# intermine tests

cd $BUILD_PROJ
ant -lib /software/noarch/junit/ default >> $ARCHIVE_TO/ant_log.txt 2>> $STD_ERR
BUILD_RESULT=$?

ant -lib /software/noarch/junit/ $TARGET >> $ARCHIVE_TO/ant_log.txt 2>> $STD_ERR


# bio tests

cd $BUILD_PROJ/../../bio/test-all
INTERMINE_RESULTS_DIR=../intermine/all/build/test/results
ant -lib /software/noarch/junit/ fulltest -Dtest.results.dir=$INTERMINE_RESULTS_DIR -Dresults.junit=$INTERMINE_RESULTS_DIR >> $ARCHIVE_TO/ant_log.txt 2>> $STD_ERR



cd $BUILD_PROJ
grep '\[junit]' $STD_ERR | grep FAILED
TEST_RESULT=$?
grep 'BUILD FAILED' $STD_ERR
BUILD_BROKEN_STATUS=$?
echo "BUILD_RESULT=$BUILD_RESULT  TEST_RESULT=$TEST_RESULT"

# if flymine build fails then report won't be generated so
# we do it again here just to be safe
ant -lib /software/noarch/junit/ test-report

ant checkstyle >> $ARCHIVE_TO/ant_log.txt
CHECKSTYLE_RESULT=$?

if [ $BUILD_RESULT -eq 0 ]; then
  echo '*** BUILD SUCCESS ***'
else
  echo '*** build failed - see log for errors ***'
fi

if [ $BUILD_BROKEN_STATUS -ne 0 ]; then
  echo '*** TESTS COMPILED ***'
else
  echo '*** tests failed to compile - see log for errors ***'
fi

if [ $TEST_RESULT -ne 0 ]; then
  echo '*** TEST SUCCESS ***'
else
  echo '*** tests failed - see log for errors ***'
fi

if [ $BUILD_BROKEN_STATUS -ne 0 ]; then
  BUILD_BROKEN=0
else
  BUILD_BROKEN=1
fi

if [ $CHECKSTYLE_RESULT -ne 0 ]; then
    CHECKSTYLE_STATUS=" (AND CHECKSTYLE FAILURES)"
else
    CHECKSTYLE_STATUS=""
fi

rm -rf $LATEST_DIR/*
if [ -f $BUILD_PROJ/build/test/results/index.html ]; then
  mkdir $ARCHIVE_TO/junit
  cp -R $BUILD_PROJ/build/test/results/* $ARCHIVE_TO/junit/
  cp -R $BUILD_PROJ/build/test/results/* $LATEST_DIR/
else
  echo "There don't seem to be any results!"
fi


if [ -f $BUILD_PROJ/build/checkstyle/index.html ]; then
  mkdir $ARCHIVE_TO/checkstyle
  cp -R $BUILD_PROJ/build/checkstyle/* $ARCHIVE_TO/checkstyle/
else
  echo "There don't seem to be any checkstyle results!"
fi

# Blame people via email
grep '\[junit]' $STD_ERR | grep FAILED > $ARCHIVE_TO/junit_failures.txt
FAILED=$?

touch $JUNIT_FAIL_FILE
echo "Emailing $BLAME..."
printf "JUnit results: $URL_PREFIX/$TIME_STAMP/junit/\n\n" > MSG
printf "Checkstyle results: $URL_PREFIX/$TIME_STAMP/checkstyle/\n\n" >> MSG
printf "Ant output: $URL_PREFIX/$TIME_STAMP/ant_log.txt\n\n" >> MSG
printf "Last update:\n\n" >> MSG
echo $UPDATE >> MSG
printf "\n\n------------------------------------------------------------\nTest failures now:\n\n" >> MSG
cat $ARCHIVE_TO/junit_failures.txt >> MSG
printf "\n\n------------------------------------------------------------\nPrevious test failures:\n\n" >> MSG
cat $JUNIT_FAIL_FILE >> MSG
printf "\n\n------------------------------------------------------------\nTest differences:\n\n" >> MSG
diff $JUNIT_FAIL_FILE $ARCHIVE_TO/junit_failures.txt >> MSG
printf "\n\n------------------------------------------------------------\nstderr output:\n\n" >> MSG

if [ $BUILD_BROKEN -eq 1 -a $FAILED -ne 0 ]; then
  cat MSG | mail -s "[BruiseControl] Build BROKEN at $TIME_STAMP $CHECKSTYLE_STATUS" "$BLAME"
elif [ $FAILED -eq 0 ]; then
  cat MSG | mail -s "[BruiseControl] Tests FAILING at $TIME_STAMP $CHECKSTYLE_STATUS" "$BLAME"
else
  cat MSG | mail -s "[BruiseControl] SUCCESS at $TIME_STAMP $CHECKSTYLE_STATUS" "$BLAME"
fi
    
# Update previous failures file
cat $ARCHIVE_TO/junit_failures.txt > $JUNIT_FAIL_FILE

TIME_NOW=`date '+%y-%m-%d_%H:%M'`
echo "*** Finished build $TIME_STAMP at $TIME_NOW ***"

rm "$RUNNING_FILE"
