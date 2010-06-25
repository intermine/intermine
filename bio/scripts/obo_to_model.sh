#!/bin/bash

if [ $# -lt 3 ]
then
    echo "expecting: $0 mine ontology namespace"
    echo "eg. flymine so org.intermine.bio so_terms ";
    exit
fi

whichmine=$1
export newModelName=$2
namespace=$3

svnpath="model_update"

export INTERMINE=~/svn/$svnpath/intermine
export MINE=~/svn/$svnpath/$whichmine
export BIO=~/svn/$svnpath/bio

CP=$CLASSPATH

CP=$CP:$INTERMINE/objectstore/main/dist/intermine-objectstore.jar
for i in $INTERMINE/objectstore/main/lib/*.jar ; do
    CP=$CP:$i
done
CP=$CP:$BIO/core/main/dist/genomic-tasks.jar
for i in $BIO/core/main/lib/*.jar ; do
    CP=$CP:$i
done

if test -z "$LD_LIBRARY_PATH" ; then
    export LD_LIBRARY_PATH=/usr/lib:/lib:/usr/lib/jni
else
    export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/lib:/lib:/usr/lib/jni
fi

#echo "Using classpath: $CP"
#echo "Using LD_LIBRARY_PATH: $LD_LIBRARY_PATH"

oboFileName=$BIO/sources/so/$newModelName.obo
buildDir=$MINE/dbmodel/build/model
modelFileName=$buildDir/${newModelName}_model.xml

./classes_in_model $whichmine
fileName=$buildDir/so_terms.txt

java -cp $CP -Xmx1000M org.intermine.bio.ontology.OboToModel $newModelName $oboFileName $modelFileName $namespace $filename

