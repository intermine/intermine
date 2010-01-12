#!/bin/bash
#
# default usage: savestat.sh relNr 
#

if [ -z "$1" ]
then
echo
echo "Please enter the release number"
echo "for example:"
echo "savestat 16"
echo
exit
fi

echo
echo -n "Please specify the host of the modMine release: ->modprod?"
echo
read RH

echo
echo -n "Please specify the host of the stat db: ->modprod?"
echo
read SH

RHOST=modprod$RH
SHOST=modprod$SH
STATDB=modstat
RELDB=modmine-r$1
USER=modmine


echo "Saving stat data for $RELDB on $RHOST to $STATDB on $SHOST"
echo
echo "Press return to continue (^C to exit).."
echo -n "->"
read

psql -q -d  $STATDB -h $SHOST -U $USER -c "delete from stage;"

psql -q -A -t -d  $RELDB -h $RHOST -U $USER -c "select dccid, '$1' from submission;" | psql -q -d  $STATDB -h $SHOST -U $USER -c "copy stage from STDIN WITH DELIMITER '|';"

psql -q -d  $STATDB -h $SHOST -U $USER -c "insert into t2 (select * from stage where dccid not in (select dccid from t2));"

