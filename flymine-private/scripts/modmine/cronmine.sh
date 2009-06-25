#!/bin/bash
#
# default usage: cron job
#
# note: a lock file is used to avoid running it when a release is being built
#
# sc 06/09
#
# TODO : test
#        modify release-mine to add removal of lock file.
PROPDIR=$HOME/.intermine
MINEDIR=$HOME/svn/trunk/modmine

if [ "$USER" != "modminebuild" ]
then
echo
echo "Please run this command as user modminebuild."
echo
exit;
fi


if [ -e $PROPDIR/build.lock ]
then
echo
echo "EXITING: release being built."
echo
exit;
fi

cd $MINEDIR
pwd

../flymine-private/scripts/modmine/automine.sh -V
