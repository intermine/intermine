#!/bin/sh

ARCHIVES_DIR="/home/bruiser/public_html/tests/archived"

cd $ARCHIVES_DIR

OLD_DIRS=`find . -maxdepth 1 -type d -name 20\* -ctime +10`
if ! test -z "$OLD_DIRS" ; then
    OLD_DIRS=`find . -maxdepth 1 -type d -name 20\* -ctime +3 | sed -e "s/^.\///" | sort`
    echo "`date`  There are `echo "$OLD_DIRS" | wc -l` old test result directories: `echo $OLD_DIRS`" 1>&2
    echo "`date`  There are `echo "$OLD_DIRS" | wc -l` old test result directories: `echo $OLD_DIRS`"
    ARC_NAME=`echo $OLD_DIRS | sed -e "s/^[^ ]*\(2[0-9][0-9][0-9]-[01][0-9]-[0123][0-9]T[012][0-9]\):\([0-5][0-9]\):\([0-5][0-9]\) .*\(2[0-9][0-9][0-9]-[01][0-9]-[0123][0-9]T[012][0-9]\):\([0-5][0-9]\):\([0-5][0-9]\)$/\1-\2-\3--\4-\5-\6/"`
    tar --remove-files -cf $ARC_NAME.tar $OLD_DIRS
    echo "`date`  Moved files to $ARC_NAME.tar of size `ls -lh $ARC_NAME.tar | sed -e "s/^.*bruiser flymine \([^ ]*\) .*$/\1/"`" 1>&2
    echo "`date`  Moved files to $ARC_NAME.tar of size `ls -lh $ARC_NAME.tar | sed -e "s/^.*bruiser flymine \([^ ]*\) .*$/\1/"`"
    rzip $ARC_NAME.tar
    echo "`date`  Compressed to $ARC_NAME.tar.rz of size `ls -lh $ARC_NAME.tar.rz | sed -e "s/^.*bruiser flymine \([^ ]*\) .*$/\1/"`" 1>&2
    echo "`date`  Compressed to $ARC_NAME.tar.rz of size `ls -lh $ARC_NAME.tar.rz | sed -e "s/^.*bruiser flymine \([^ ]*\) .*$/\1/"`"
fi

rm /home/bruiser/public_html/tests/trunk/intermine/all/executeLog
