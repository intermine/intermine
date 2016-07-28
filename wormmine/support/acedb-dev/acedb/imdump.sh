#!/bin/bash

# DEPRECATED! This has been replaced by scripts/dump_ace.sh

# expects models file

if [ $# -ne 1 ]; then
    echo "Usage: $0 WSXXX"
    echo ""
    echo "Dumps files to /mnt/ephemeral0/intermine-builds/WSXXX"
    exit 1
fi

VERSION=$1
# Database directory: created dynamically based on version.
ACEDB_BASE=/usr/local/wormbase/acedb
ACEDB_BIN=${ACEDB_BASE}/bin
ACEDB_DATA=${ACEDB_BASE}/wormbase_{$version}
DUMPDIR="/mnt/ephemeral0/intermine-builds/$version"

if [ ! -e "models" ]; then
    echo Cannot find models file.
    exit 1
fi

if [ ! -e "$DUMPDIR" ]; then
mkdir -p $DUMPDIR 
fi

for model in `cat models`
do
    if [ ! -e "$DUMPDIR/$model.xml" ]
    then
        echo $model
        $ACEDB_BIN/tace "$ACEDB_DATA" <<EOF > /dev/null
wb

find ${model}
show -x -f "$DUMPDIR/$model.xml"
EOF
        echo ... done.
    fi
done
