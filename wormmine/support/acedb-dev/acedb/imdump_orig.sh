#!/bin/bash

# expects models file

dumpdir="imdumps";

if [ ! -e "models" ]; then
    echo Cannot find models file.
    exit 1
fi

if [ -z "$ACEDB" ]; then
    ACEDB="/usr/local/wormbase/acedb/wormbase"
    echo 'Did not specify database dir in $ACEDB. Using ' $ACEDB
fi

if [ ! -e "$dumpdir" ]; then
mkdir imdumps
fi

for model in `cat models`
do
    if [ ! -e "$dumpdir/$model.jace" ]
    then
        echo $model
        tace "$ACEDB" <<EOF > /dev/null
wb

find ${model}
show -x -f "$dumpdir/$model.jace"
EOF
        echo ... done.
    fi
done
