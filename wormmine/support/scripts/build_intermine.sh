#!/bin/bash

cwd="`pwd`"
release="$1"

if [ "$release" == "" ] ; then
  echo "One parameter needed: wormbase release"
  echo ""
  echo "Example: ./website-intermine/scripts/build_intermine.sh WS240"
  exit 1
fi

release=`echo "$release" | tr '[:lower:]' '[:upper:]'`
database=`echo "wormmine_$release" | tr '[:upper:]' '[:lower:]'`

echo "USING RELEASE: $release"
echo "DATABASE NAME: $database"

if [ -d acedb-dev ] ; then
  cd acedb-dev/intermine/wormmine
elif [ -d website-intermine ] ; then
  cd website-intermine/acedb-dev/intermine/wormmine
else
  echo "Please execute this script with either:"
  echo ""
  echo "  1. the directory 'website-intermine' in the current working directory, or"
  echo "  2. within the 'website-intermine' directory."
  exit 2
fi

echo "Updating ~/.intermine/wormmine.properties..."

if [ -f ~/.intermine/wormmine.properties.tmp ] ; then
  echo "Woops... the temporary file ~/.intermine/wormmine.properties.tmp exists already."
  echo ""
  echo "Please delete this file to proceed."
  exit 2
fi

sed s/^db.production.datasource.databaseName=.*/db.production.datasource.databaseName=$database/ ~/.intermine/wormmine.properties > ~/.intermine/wormmine.properties.tmp
mv ~/.intermine/wormmine.properties.tmp ~/.intermine/wormmine.properties

echo "Updating project.xml..."

if [ -f project.xml.tmp ] ; then
  echo "Woops... the temporary file project.xml.tmp exists already."
  echo ""
  echo "Please delete this file to proceed."
  exit 3
fi

sed s/gene_ontology\.WS.*\.obo/gene_ontology.$release.obo/ project.xml > project.xml.tmp
mv project.xml.tmp project.xml

echo "Building the InterMine database..."

../bio/scripts/project_build -l -v localhost $database

