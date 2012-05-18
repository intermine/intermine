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

# get path (cronjob does not do it!)
. $HOME/.bashrc

if [ "$USER" != "modminebuild" ]
then
echo "`date +%y%m%d.%H%M`: EXITING: please run this command as user modminebuild."
exit;
fi


if [ -e $PROPDIR/build.lock ]
then
echo "`date +%y%m%d.%H%M`: EXITING: release being built."
exit;
fi

cd $MINEDIR
pwd
echo "`date +%y%m%d.%H%M`: RUNNING automatic validation."
../flymine-private/scripts/modmine/automine.sh -V
