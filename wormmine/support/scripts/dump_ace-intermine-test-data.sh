#!/bin/bash

# Dump data from ACeDB for limited objects in XML format.

CWD="`pwd`"
VERSION="$1"

if [ ! $VERSION ] ; then
  echo ""
  echo "Generate XML dumps to create a test set of data for Intermine."
  echo "You must provide the WSXXX version you wish to build."
  echo ""
  echo "Example: $0 WS246"
  exit 1
fi

ACEDB_BASE=/usr/local/wormbase/acedb
ACEDB_BIN=${ACEDB_BASE}/bin
ACEDB_DATA=${ACEDB_BASE}/wormbase_${VERSION}

DESTINATION=/usr/local/wormbase/intermine/builds/$VERSION/${VERSION}-test-data

# Create the destination directory
if [ ! -e "$DESTINATION" ]; then
    mkdir -p $DESTINATION
fi


GENES=("unc-26"  "daf-2" "egl-15" "mir-1" "snt-1") 

for GENE in ${GENES[@]}; do
    
    echo "Dumping xml for $GENE..."
    $ACEDB_BIN/tace "$ACEDB_DATA" <<EOF > /dev/null
wb
query find Gene_name $GENE ; follow Public_name_for
show -x -f "$DESTINATION/temp-Gene-$GENE.xml"
query find Gene_name $GENE ; follow Public_name_for ; follow Corresponding_transcript
show -x -f "$DESTINATION/temp-Transcript-$GENE.xml"
query find Gene_name $GENE ; follow Public_name_for ; follow Corresponding_transcript ; follow Corresponding_CDS
show -x -f "$DESTINATION/temp-CDS-$GENE.xml"
query find Gene_name $GENE ; follow Public_name_for ; follow Corresponding_transcript ; follow Corresponding_CDS ; follow Corresponding_protein
show -x -f "$DESTINATION/temp-Protein-$GENE.xml"
query find Gene_name $GENE ; follow Public_name_for ; follow Allele
show -x -f "$DESTINATION/temp-Variation-$GENE.xml"
query find Gene_name $GENE ; follow Public_name_for ; follow Expr_pattern
show -x -f "$DESTINATION/temp-Expr_pattern-$GENE.xml"
query find Gene_name $GENE ; follow Public_name_for ; follow Expression_cluster
show -x -f "$DESTINATION/temp-Expression_cluster-$GENE.xml"
query find Gene_name $GENE ; follow Public_name_for ; follow Expr_pattern ; follow Life_stage
show -x -f "$DESTINATION/temp-Life_stage-$GENE.xml"
query find Gene_name $GENE ; follow Public_name_for ; follow Expr_pattern ; follow Anatomy_term
show -x -f "$DESTINATION/temp-Anatomy_term-$GENE.xml"
query find Gene_name $GENE ; follow Public_name_for ; follow Allele ; follow Phenotype       
show -x -f "$DESTINATION/temp-Phenotype-$GENE.xml"
query find Gene_name $GENE ; follow Public_name_for ; follow Species
show -x -f "$DESTINATION/temp-Species-$GENE.xml"
query find Gene_name $GENE ; follow Public_name_for ; follow Allele; follow Phenotype ; follow RNAi
show -x -f "$DESTINATION/temp-RNAi-$GENE.xml"

EOF

done


cd $DESTINATION
echo "Concatenating files..."
cat temp-Gene*       > Gene.xml
cat temp-Transcript* > Transcript.xml
cat temp-CDS*        > CDS.xml
cat temp-Protein*    > Protein.xml
cat temp-Expr_pattern* > Expr_pattern.xml
cat temp-Expression_cluster*        > Expression_cluster.xml
cat temp-Variation*  > Variation.xml
cat temp-Life_stage* > Life_stage.xml
cat temp-Phenotype*  > Phenotype.xml
cat temp-Species*  > Species.xml
cat temp-RNAi* > RNAi.xml
rm -f temp*
echo ... done.
cd ../
tar czf  ${VERSION}-test-data.tgz ${VERSION}-test-data
cd "$CWD"









