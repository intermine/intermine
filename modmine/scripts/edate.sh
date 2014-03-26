#!/bin/bash

DBHOST=modprod1
DBUSER=modmine
REFFILE=/shared/data/modmine/subs/chado/ftplist


if [ -z "$1" ]
then
echo
echo -n "Please enter project name: ->"
echo
read PRO
else
PRO="$1"
fi


psql -h $DBHOST -d modchado-$PRO -U $DBUSER -q -t -A -F ' ' -c "select value, experiment_id from experiment_prop where name = 'dcc_id' except
select value, experiment_id  from experiment_prop where name = 'dcc_id' and experiment_id in
(select distinct experiment_id from experiment_prop where name = 'Embargo Date');" > $PRO.ed


LOOPVAR=`cat $PRO.ed | awk '{print $1}'`

echo $LOOPVAR

CVTERM=`psql -h $DBHOST -d modchado-$PRO -U $DBUSER -q -t -c "select cvterm_id from cvterm where name = 'date';"`

echo "+++++++++++++"
echo $CVTERM
echo "+++++++++++++"

for item in $LOOPVAR
do

EID=`grep -w ^$item $PRO.ed | awk '{print $2}'`

EDATE=`grep -w ^$item $REFFILE | sed -n 's/.*\t//;p'`

echo $item $EDATE $EID

psql -h  $DBHOST -d modchado-$PRO -U $DBUSER -c "insert into experiment_prop (experiment_id, name, value, type_id) values ($EID, 'Embargo Date', '$EDATE', $CVTERM);"

done

echo "modchado-$PRO done"
exit

