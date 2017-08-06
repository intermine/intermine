#!/bin/bash

# Dump data from ACeDB in XML format.

CWD="`pwd`"
VERSION="$1"

if [ ! $VERSION ] ; then
  echo ""
  echo "Generate XML dumps to begin a build of intermine."
  echo "You must provide the WSXXX version you wish to build."
  echo ""
  echo "Example: ./scripts/dump_ace.sh WS246"
  exit 1
fi

ACEDB_BASE=/usr/local/wormbase/acedb
ACEDB_BIN=${ACEDB_BASE}/bin
ACEDB_DATA=${ACEDB_BASE}/wormbase_${VERSION}

# Assuming that we are on AWS...
#DESTINATION=/mnt/ephemeral0/intermine-builds/$VERSION
DESTINATION=/usr/local/wormbase/intermine/builds/$VERSION

# Create the destination directory
if [ ! -e "$DESTINATION" ]; then
    mkdir -p $DESTINATION
fi

# Cd to the acedb-dev directory in order to find our models.
if [ -d acedb-dev ] ; then
    cd acedb-dev/acedb
else
  echo "This script should be executed from the website-intermine/ root level."
  exit 2
fi


# Do a straight dump of specific classes based on the models file
if [ -e "models.constrained" ]
then
    echo "using constrained models file... models.constrained"
    models="models.constrained"
elif [ -e "models.all" ]
then
    models="models"
else
    echo "Cannot find a suitable models file"
    exit 1
fi

MODELS=( Gene )

for model in `cat ${models}`
#for model in $MODELS
do
    echo $model

   # Some classes require special processing. 
    if [ "$model" == 'Gene' ]
    then		
	query="query find Gene Live"
    elif [ "$model" == 'Protein' ]
    then
	query="query find Protein Corresponding_CDS"
    elif [ "$model" == 'CDS' ]
    then
	query="query find CDS Method=\"curated\""
    elif [ "$model" == 'Transcript' ]
    then
	query="query find Transcript (Gene)"
    elif [ "$model" == 'Species' ]
    then
	query="KeySet-Read acedb-dev/acedb/species.ace"
    else 
	query="find ${model}"
    fi
    
	echo "Dumping $model using query: $query"
    $ACEDB_BIN/tace "$ACEDB_DATA" <<EOF > /dev/null
wb
$query
show -x -f "$DESTINATION/$model.xml"
EOF

    cd $DESTINATION
    gzip $model.xml
    echo ... done.
    cd "$CWD"
done


# Some classes require custom queries to simplify the resulting XML.
# The invocation below is the same as running commmands in tace
# from the command line:
#
# acedb> query find Gene Live
# acedb> show -x -f <ACE XML DUMP>/Gene.xml
# etc...

# We either need to remove or overwrite the previous entries

#echo -e "query find Gene Live\nshow -x -f $DESTINATION/Gene.xml\nquery find Protein Corresponding_CDS\nshow -x -f $DESTINATION/Protein.xml\nquery find CDS Meth#od=\"curated\"\nshow -x -f $DESTINATION/CDS.xml\nquery find Transcript (Gene)\nshow -x -f $DESTINATION/Transcript.xml\nKeySet-Read acedb-dev/acedb/species.ace\#nshow -x -f $DESTINATION/Species.xml" | ${ACEDB_BIN}/tace ${ACEDB_DATA}
#chmod g+w $DESTINATION
#cd $DESTINATION

cd "$CWD"

