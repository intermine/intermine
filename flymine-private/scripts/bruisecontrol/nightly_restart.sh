#!/bin/sh
/home/bruiser/apache-tomcat-5.5.25/bin/shutdown.sh
sleep 30
/home/bruiser/apache-tomcat-5.5.25/bin/startup.sh

OLD_DIRS=`find /home/bruiser/public_html/tests/archived -maxdepth 1 -name 20\* -atime +30`
if ! test -z "$OLD_DIRS" ; then
    echo "There are some old test result directories:"
    find /home/bruiser/public_html/tests/archived -maxdepth 1 -name 20\* -atime +30
fi
