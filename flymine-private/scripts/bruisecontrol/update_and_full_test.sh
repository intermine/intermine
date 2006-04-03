RUNNING_FILE=~/public_html/tests/.running
TRUNK_DIR=~/public_html/tests/trunk
BUILD_PROJ=$TRUNK_DIR/intermine/all
ARCHIVED_DIR=~/public_html/tests/archived
TIME_STAMP=`date '+%y%m%d_%H%M'`
ARCHIVE_TO=$ARCHIVED_DIR/$TIME_STAMP
LATEST_DIR=$ARCHIVED_DIR/latest
STD_ERR=~/public_html/tests/stderr.log
JUNIT_FAIL_FILE=~/public_html/tests/previous_junit_failures

. ~/.bashrc

# -------------------------------------------------------------------------- #
# Check whether we really need to do anything at all
# -------------------------------------------------------------------------- #

if [ -f "$RUNNING_FILE" ]; then
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
  echo "Need to wait $((60*10-$DIFF)) more seconds"
  if [ "$#" -eq "0" ]; then
    exit
  fi
else
  echo "10 minutes have pasted since last update - lets do it..."
fi

touch "$RUNNING_FILE"

# -------------------------------------------------------------------------- #
# Find out who to blame when things go wrong
# -------------------------------------------------------------------------- #

#BLAME=$(svn status -v -u | grep '^.......\*' | cut -c 28-40 | sort -u)
NEXT=$(($(svn info | grep "Revision" | cut -c 11-)+1))
BLAME=$(svn log -r $NEXT:HEAD | grep '^r[0-9]' | awk '{print $3;}' | sort | uniq | xargs | perl -p -e 's/\s/\@flymine.org, /g;')
echo "BLAME = $BLAME"

umask 0022
mkdir -p "$ARCHIVE_TO"
mkdir -p "$LATEST_DIR"

svn update svn://svn.flymine.org/flymine/trunk $TRUNK_DIR

cd $BUILD_PROJ
ant clean-all
cd $TRUNK_DIR
UPDATE=$(svn up)
cd $BUILD_PROJ

if [ "$#" -eq "0" ]; then
  TARGET=fulltest
else
  TARGET=$1
fi

dropdb unittest
dropdb testmodel-webapp-userprofile
dropdb testmodel-webapp
dropdb notxmltest
dropdb truncunittest
dropdb fulldatatest
dropdb flatmodetest
dropdb genomictest
dropdb webservice-test

createdb webservice-test
createdb unittest
createdb testmodel-webapp-userprofile
createdb testmodel-webapp
createdb notxmltest
createdb truncunittest
createdb fulldatatest
createdb flatmodetest
createdb genomictest

cd ../../testmodel/dbmodel
ant build-db > "$ARCHIVE_TO/ant_log.txt" 2> "$STD_ERR"
cd $BUILD_PROJ

ant -lib /software/noarch/junit/ default > "$ARCHIVE_TO/ant_log.txt" 2> "$STD_ERR"
BUILD_RESULT=$?

ant -lib /software/noarch/junit/ $TARGET >> "$ARCHIVE_TO/ant_log.txt" 2>> "$STD_ERR"
cd $BUILD_PROJ/../../flymine
ant -lib /software/noarch/junit/ fulltest -Dresults.junit=../intermine/all/build/test/results >> "$ARCHIVE_TO/ant_log.txt" 2>> "$STD_ERR"
cd $BUILD_PROJ
cat "$STD_ERR" | grep '\[junit]'
TEST_RESULT=$?
cat "$STD_ERR" | grep 'BUILD FAILED'
BUILD_BROKEN=$?
echo "BUILD_RESULT=$BUILD_RESULT  TEST_RESULT=$TEST_RESULT"

# if flymine build fails then report won't be generated so
# we do it again here just to be safe
ant -lib /software/noarch/junit/ test-report

ant checkstyle >> "$ARCHIVE_TO/ant_log.txt"
CHECKSTYLE_RESULT=$?

if [ $BUILD_RESULT -eq 0 ]; then
  echo '*** BUILD SUCCESS ***'
else
  echo '*** build failed - see log for errors ***'
fi

if [ $BUILD_BROKEN -ne 0 ]; then
  echo '*** TESTS COMPILED ***'
else
  echo '*** tests failed to compile - see log for errors ***'
fi

if [ $TEST_RESULT -ne 0 ]; then
  echo '*** TEST SUCCESS ***'
else
  echo '*** tests failed - see log for errors ***'
fi

if [ $BUILD_BROKEN -ne 0 ]; then
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
if [ -f "$BUILD_PROJ/build/test/results/index.html" ]; then
  mkdir "$ARCHIVE_TO/junit"
  cp -R $BUILD_PROJ/build/test/results/* "$ARCHIVE_TO/junit/"
  cp -R $BUILD_PROJ/build/test/results/* "$LATEST_DIR/"
else
  echo "There don't seem to be any results!"
fi


if [ -f "$BUILD_PROJ/build/checkstyle/index.html" ]; then
  mkdir "$ARCHIVE_TO/checkstyle"
  cp -R $BUILD_PROJ/build/checkstyle/* "$ARCHIVE_TO/checkstyle/"
else
  echo "There don't seem to be any checkstyle results!"
fi

#if [ $BUILD_RESULT -ne 0 ]; then
  # Email to say the build failed
#  printf "The build failed!\n\n" > MSG
#  echo "Full ant log file: http://bc.flymine.org/~bruiser/tests/archived/$TIME_STAMP/" >> MSG
#  printf "\n\nLast check-in:\n\n" >> MSG
#  echo "$UPDATE" >> MSG
#  printf "\n\nTail of log file:\n\n" >> MSG
#  tail -n 100 "$ARCHIVE_TO/ant_log.txt" >> MSG
#  cat MSG | mail -s "[BruiseControl] Build failed at $TIME_STAMP" bruiser@flymine.org
#else
  # Email test results
  # Blame people via email
  cat "$STD_ERR" | grep '\[junit]' > "$ARCHIVE_TO/junit_failures.txt"
  FAILED=$?
  touch "$JUNIT_FAIL_FILE"
#  DIFF=$(diff -B "$ARCHIVE_TO/junit_failures.txt" "$JUNIT_FAIL_FILE")
#  if [ $? -eq 0 ]; then
#    echo 'failure set is equal'
#  else
#    echo "failures set is not equal - should email $BLAME"
     echo "Emailing $BLAME..."
#  BLAME=bruiser
    printf "JUnit results: http://bc.flymine.org/~bruiser/tests/archived/$TIME_STAMP/junit/\n\n" > MSG
    printf "Checkstyle results: http://bc.flymine.org/~bruiser/tests/archived/$TIME_STAMP/checkstyle/\n\n" >> MSG
    printf "Ant output: http://bc.flymine.org/~bruiser/tests/archived/$TIME_STAMP/ant_log.txt\n\n" >> MSG
    printf "Last update:\n\n" >> 'MSG'
    echo "$UPDATE" >> 'MSG'
    #printf "\n\n------------------------------------------------------------\nTest failures diff:\n\n$DIFF" >> 'MSG'
    printf "\n\n------------------------------------------------------------\nTest failures now:\n\n" >> 'MSG'
    cat "$ARCHIVE_TO/junit_failures.txt" >> 'MSG'
    printf "\n\n------------------------------------------------------------\nPrevious test failures:\n\n" >> 'MSG'
    cat "$JUNIT_FAIL_FILE" >> 'MSG'
    printf "\n\n------------------------------------------------------------\nstderr output:\n\n" >> 'MSG'
    cat "$STD_ERR" >> 'MSG'
    if [ $BUILD_BROKEN -eq 1 -a $FAILED -ne 0 ]; then
      cat MSG | mail -s "[BruiseControl] Build BROKEN at $TIME_STAMP $CHECKSTYLE_STATUS" "$BLAME"
    elif [ $FAILED -eq 0 ]; then
      cat MSG | mail -s "[BruiseControl] Tests FAILING at $TIME_STAMP $CHECKSTYLE_STATUS" "$BLAME"
    else
      cat MSG | mail -s "[BruiseControl] SUCCESS at $TIME_STAMP $CHECKSTYLE_STATUS" "$BLAME"
    fi
    
    # Update previous failures file
    cat "$ARCHIVE_TO/junit_failures.txt" > "$JUNIT_FAIL_FILE"
#  fi
#fi

TIME_NOW=`date '+%y-%m-%d_%H:%M'`
echo "*** Finished build $TIME_STAMP at $TIME_NOW ***"

#rm "$STD_ERR"
rm "$RUNNING_FILE"
