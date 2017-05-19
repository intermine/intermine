#!/bin/bash

#Paulo Nuin Jan 2015, modified Feb 15 - Aug 2016

# TODO: set release version as a script argument
# TODO: add mapping/property files to repo and copy process here
# TODO: not process XML files already processed

#set the version to be accessed
wbrel="WS254"
echo 'Release version' $wbrel


#################### Species ####################
#                                               #
#  species to be transferred                    # 
#  key:value structure with species             #
#  "name" and BioProject number                 #
#  required in order to get the right           #  
#  directory and file                           #
#                                               #
#################### Species #################### 
declare -A species=(["c_elegans"]="PRJNA13758" 
                    ["b_malayi"]="PRJNA10729" 
                    ["c_angaria"]="PRJNA51225" 
                    ["c_brenneri"]="PRJNA20035"
                    ["c_briggsae"]="PRJNA10731" 
                    ["c_japonica"]="PRJNA12591" 
                    ["c_remanei"]="PRJNA53967" 
                    ["c_tropicalis"]="PRJNA53597"
                    ["o_volvulus"]="PRJEB513" 
                    ["p_pacificus"]="PRJNA12644" 
                    ["p_redivivus"]="PRJNA186477" 
                    ["s_ratti"]="PRJEB125"
                    ["c_sinica"]="PRJNA194557")

# sourcedir='/mnt/data/acedb_dumps/'$wbrel'' # <---- XML dump location
sourcedir='/Users/nuin/intermine_work/WS254-test-data'

#################### Main dirs ##################
#                                               #
#  datadir - main data directory                # 
#  acexmldir - subdir for AceDB XML files       #
#  pp - pre-processing dir with perl and bash   #
#                                               #
#################### Species ####################
# intermine='/mnt/data/intermine'
intermine='/Users/nuin/intermine_work/new/intermine' #local test
datadir=$intermine'/datadir'   # for now the datadir is inside the intermine directory
acexmldir=$datadir'/wormbase-acedb'
testlab=$intermine'/wormmine/support/scripts/testlab'


#################### FTP ########################
#################### Species #################### 
for spe in "${!species[@]}"
do
  echo species: $spe ${species["$spe"]}

  #################### get the genomic data ####################
  echo 'Getting genomic data'
  mkdir -vp $datadir'/fasta/'$spe"/genomic"
  cd $datadir'/fasta/'$spe"/genomic"
  if [ ! -f "$spe"."${species["$spe"]}"."$wbrel".genomic.fa ]; then
    echo "$spe"."${species["$spe"]}"."$wbrel".genomic.fa 'not found'
    echo 'transferring ' "$spe"."${species["$spe"]}"."$wbrel".genomic.fa.gz
    wget -O "$spe"."${species["$spe"]}"."$wbrel".genomic.fa.gz "ftp://206.108.120.212/pub/wormbase/releases/"$wbrel"/species/"$spe"/"${species["$spe"]}"/"$spe"."${species["$spe"]}"."$wbrel".genomic.fa.gz"
    gunzip  -v "$spe"."${species["$spe"]}"."$wbrel".genomic.fa.gz
  else
    echo "$spe"."${species["$spe"]}"."$wbrel".genomic.fa 'found, not transferring'
  fi

  #################### get the protein data ####################
  echo 'Getting protein data'
  mkdir -vp $datadir"/fasta/"$spe"/proteins/raw"
  mkdir -vp $datadir"/fasta/"$spe"/proteins/prepped"
  cd $datadir"/fasta/"$spe"/proteins/raw"
  if [ ! -f "$spe"."${species["$spe"]}"."$wbrel".protein.fa ]; then
    echo "$spe"."${species["$spe"]}"."$wbrel".protein.fa 'not found'
    echo 'transferring ' "$spe"."${species["$spe"]}"."$wbrel".protein.fa
    wget -O "$spe"."${species["$spe"]}"."$wbrel".protein.fa.gz "ftp://206.108.120.212/pub/wormbase/releases/"$wbrel"/species/"$spe"/"${species["$spe"]}"/"$spe"."${species["$spe"]}"."$wbrel".protein.fa.gz"
    gunzip -v "$spe"."${species["$spe"]}"."$wbrel".protein.fa.gz
  else
    echo "$spe"."${species["$spe"]}"."$wbrel".protein.fa 'found, not transferring'
  fi
  perl $testlab'/perl/preprocess/fasta/wb-proteins/prep-wb-proteins.pl' "$spe"."${species["$spe"]}"."$wbrel".protein.fa ../prepped/"$spe"."${species["$spe"]}"."$wbrel".protein.fa

  # #################### get gff annotations ####################
  echo 'Getting gff data'
  mkdir -vp $datadir'/wormbase-gff3/raw'
  mkdir -vp $datadir'/wormbase-gff3/final'
  cd $datadir'/wormbase-gff3'
  if [ ! -f raw/"$spe"."${species["$spe"]}"."$wbrel".gff ]; then
    echo 'transferring' "$spe"."${species["$spe"]}"."$wbrel".gff
    wget -O raw/"$spe"."${species["$spe"]}"."$wbrel".gff.gz  "ftp://206.108.120.212/pub/wormbase/releases/"$wbrel"/species/"$spe"/"${species["$spe"]}"/"$spe"."${species["$spe"]}"."$wbrel".annotations.gff3.gz"
    gunzip -v raw/"$spe"."${species["$spe"]}"."$wbrel".gff.gz
    bash $testlab'/perl/preprocess/gff3/scrape_gff3.sh' $datadir/wormbase-gff3/raw/"$spe"."${species["$spe"]}"."$wbrel".gff $datadir/wormbase-gff3/final/"$spe"."${species["$spe"]}"."$wbrel".gff
  else
    echo  raw/"$spe"."${species["$spe"]}"."$wbrel".gff 'found'
  fi
done


#################### gene ontology ####################
mkdir -vp $datadir"/go/"
if [ ! -f $datadir/go/gene_ontology."$wbrel".obo ];then
  echo 'transferring gene ontology file'
  wget -O $datadir/go/gene_ontology.1_2.obo "ftp://206.108.120.212/pub/wormbase/releases/"$wbrel"/ONTOLOGY/gene_ontology."$wbrel".obo"
else
  echo 'gene ontolgy file found'
fi

#################### gene association #################
mkdir -vp $datadir'/go-annotation/raw/'
mkdir -vp $datadir'/go-annotation/final'
if [ ! -f $datadir'/go-annotation/final/gene_association_sorted_filtered.wb' ];then
  echo 'transferring gene association file'
  wget -O $datadir'/go-annotation/raw/gene_association'."$wbrel".wb "ftp://206.108.120.212/pub/wormbase/releases/"$wbrel"/ONTOLOGY/gene_association."$wbrel".wb"
  echo 'sorting'
  sort -k 2,2 $datadir'/go-annotation/raw/gene_association'."$wbrel".wb > $datadir'/go-annotation/raw/gene_association_sorted.wb'
  echo 'filtering'
  bash $testlab'/perl/preprocess/go-annotation/filter_out_uniprot.sh' $datadir'/go-annotation/raw/gene_association_sorted.wb' $datadir'/go-annotation/final/gene_association_sorted_filtered.wb'
else
  echo 'gene association file found'
fi



#cp /mnt/data/properties/id_mapping.tab $datadir'/wormbase-gff3/mapping/'
#cp /mnt/data/properties/typeMapping.tab $datadir'/wormbase-gff3/mapping/'
#################### AceDB processing #################

#################### anatomy term #####################
echo 'anatomy_term'
mkdir -vp $datadir/wormbase-acedb/anatomy_term/XML
mkdir -vp $datadir/wormbase-acedb/anatomy_term/mapping
cp -v $sourcedir/Anatomy_term.xml $acexmldir/anatomy_term/Anatomy_term.xml
cp -v $intermine'/wormmine/support/properties/anatomy_term_mapping.properties' $datadir'/wormbase-acedb/anatomy_term/mapping/'
perl $testlab'/perl/preprocess/wb-acedb/anatomy_term//prep_anatomy_term.pl' $datadir'/wormbase-acedb/anatomy_term/Anatomy_term.xml' $datadir'/wormbase-acedb/anatomy_term/XML/Anatomy_term_prepped.xml'

#################### cds #############################
echo 'cds'
mkdir -vp $datadir/wormbase-acedb/cds/XML
mkdir -vp $datadir/wormbase-acedb/cds/mapping
cp -v $sourcedir/CDS.xml $acexmldir/cds/CDS.xml
cp -v $intermine'/wormmine/support/properties/cds_mapping.properties' $datadir'/wormbase-acedb/cds/mapping/'
perl $testlab'/perl/purify_xace/purify_xace.pl' $datadir'/wormbase-acedb/cds/CDS.xml' $datadir'/wormbase-acedb/cds/purified_CDS.xml'
perl $testlab'/perl/preprocess/wb-acedb/cds/prep_wb-acedb-cds.pl' $datadir'/wormbase-acedb/cds/purified_CDS.xml' $datadir'/wormbase-acedb/cds/XML/prepped_CDS.xml'
rm -v $datadir/wormbase-acedb/cds/purified_CDS.xml

#################### expression cluster ##############
echo 'expression cluster'
mkdir -vp $datadir/wormbase-acedb/expr_cluster/XML
mkdir -vp $datadir/wormbase-acedb/expr_cluster/mapping
cp -v $sourcedir/Expression_cluster.xml $acexmldir/expr_cluster/Expression_cluster.xml
cp -v $intermine'/wormmine/support/properties/expr_cluster_mapping.properties' $datadir'/wormbase-acedb/expr_cluster/mapping/'
perl $testlab'/perl/purify_xace/purify_xace.pl' $datadir'/wormbase-acedb/expr_cluster/Expression_cluster.xml' $datadir'/wormbase-acedb/expr_cluster/XML/purified_expression_cluster.xml'

#################### expression pattern #############
echo 'expression pattern'
mkdir -vp $datadir/wormbase-acedb/expr_pattern/XML
mkdir -vp $datadir/wormbase-acedb/expr_pattern/mapping
cp -v $sourcedir/Expr_pattern.xml $acexmldir/expr_pattern/Expr_pattern.xml
cp -v $intermine'/wormmine/support/properties/expr_pattern_mapping.properties' $datadir'/wormbase-acedb/expr_pattern/mapping/'
perl $testlab'/perl/preprocess/wb-acedb/expr_pattern/prep_expr_pattern.pl' $datadir'/wormbase-acedb/expr_pattern/Expr_pattern.xml' $datadir'/wormbase-acedb/expr_pattern/XML/Expr_pattern_prepped.xml'

#################### gene ###########################
echo 'gene'
mkdir -vp $datadir/wormbase-acedb/gene/XML
mkdir -vp $datadir/wormbase-acedb/gene/mapping
cp -v $sourcedir/Gene.xml $acexmldir/gene/Gene.xml
cp -v $intermine'/wormmine/support/properties/wormbase-acedb-gene.properties' $datadir'/wormbase-acedb/gene/mapping/'
perl $testlab'/perl/purify_xace/purify_xace.pl' $datadir'/wormbase-acedb/gene/Gene.xml' $datadir'/wormbase-acedb/gene/purified_gene.xml'
perl $testlab'/perl/preprocess/wb-acedb/gene/prep_wb-acedb-gene.pl' $datadir'/wormbase-acedb/gene/purified_gene.xml' $datadir'/wormbase-acedb/gene/XML/prepped_gene.xml'
rm $datadir/wormbase-acedb/gene/purified_gene.xml


#################### life stage #####################
echo 'life stage'
mkdir -vp $datadir/wormbase-acedb/life_stage/XML
mkdir -vp $datadir/wormbase-acedb/life_stage/mapping
cp $sourcedir/Life_stage.xml $acexmldir/life_stage/Life_stage.xml
cp $intermine'/wormmine/support/properties/life_stage_mapping.properties' $datadir'/wormbase-acedb/life_stage/mapping/'
perl $testlab'/perl/purify_xace/purify_xace.pl' $datadir'/wormbase-acedb/life_stage/Life_stage.xml' $datadir'/wormbase-acedb/life_stage/XML/purified_life_stage.xml'


#################### phenotype #####################
echo 'phenotype'
mkdir -vp $datadir/wormbase-acedb/phenotype/XML
mkdir -vp $datadir/wormbase-acedb/phenotype/mapping
cp -v $sourcedir/Phenotype.xml $acexmldir/phenotype/Phenotype.xml
cp -v $intermine'/wormmine/support/properties/phenotype_mapping.properties' $datadir'/wormbase-acedb/phenotype/mapping'
perl $testlab'/perl/purify_xace/purify_xace.pl' $datadir'/wormbase-acedb/phenotype/Phenotype.xml' $datadir'/wormbase-acedb/phenotype/XML/purified_phenotype.xml'

#################### protein #######################
echo 'protein'
mkdir -vp $datadir/wormbase-acedb/protein/XML
mkdir -vp $datadir/wormbase-acedb/protein/mapping
cp -v $sourcedir/Protein.xml $acexmldir/protein/Protein.xml
cp -v $intermine'/wormmine/support/properties/protein_mapping.properties' $datadir'/wormbase-acedb/protein/mapping'
perl $testlab'/perl//preprocess/wb-acedb/protein/prep_wb-acedb-protein.pl' $datadir'/wormbase-acedb/protein/Protein.xml' $datadir'/wormbase-acedb/protein/prepped_protein.xml'
perl $testlab'/perl/preprocess/wb-acedb/protein/purge_protein.pl' $datadir'/wormbase-acedb/protein/prepped_protein.xml' $datadir/'wormbase-acedb/protein/XML/purged_prepped_protein.xml' $testlab'/perl/preprocess/wb-acedb/protein/whitelist/species_whitelist.txt' $datadir'/wormbase-acedb/protein/rejected_by_purge.xml'
rm $datadir/wormbase-acedb/protein/prepped_protein.xml

#################### species #####################
echo 'species'
mkdir -vp $datadir/wormbase-acedb/species/XML
mkdir -vp $datadir/wormbase-acedb/species/mapping
cp -v $sourcedir/Species.xml $acexmldir/species/Species.xml
# cp -v $intermine'/wormmine/support/species_mapping.properties' $datadir'/wormbase-acedb/species/mapping'
mkdir -p $datadir/entrez-organism/build/

#################### transcript ##################
echo 'transcript'
mkdir -vp $datadir/wormbase-acedb/transcript/XML
mkdir -vp $datadir/wormbase-acedb/transcript/mapping
cp -v $sourcedir/Transcript.xml $acexmldir/transcript/Transcript.xml
cp -v $intermine'/wormmine/support/properties/transcript_mapping.properties' $datadir'/wormbase-acedb/transcript/mapping'
perl $testlab'/perl/preprocess/wb-acedb/transcript/prep_wb-acedb-transcript.pl' $datadir'/wormbase-acedb/transcript/Transcript.xml' $datadir'/wormbase-acedb/transcript/XML/prepped_Transcript.xml'

#################### RNAi  ######################
echo 'RNAi'
mkdir -vp $datadir/wormbase-acedb/RNAi/XML
mkdir -vp $datadir/wormbase-acedb/RNAi/mapping
cp -v $sourcedir/RNAi.xml $acexmldir/RNAi/RNAi.xml
cp -v $intermine'/wormmine/support/properties/rnai_mapping.properties' $datadir'/wormbase-acedb/RNAi/mapping'
perl $testlab'/perl/preprocess/wb-acedb/RNAi/prep_RNAi.pl' $datadir'/wormbase-acedb/RNAi/RNAi.xml' $datadir'/wormbase-acedb/RNAi/XML/prepped_RNAi.xml'

#################### variation ##################
echo 'variation'
mkdir -vp $datadir/wormbase-acedb/variation/XML
mkdir -vp $datadir/wormbase-acedb/variation/mapping
cp -v $sourcedir/Variation.xml $acexmldir/variation/Variation.xml
cp -v $intermine'/wormmine/support/properties/variation_mapping.properties' $datadir'/wormbase-acedb/variation/mapping'
perl $testlab'/perl/preprocess/wb-acedb/variation/purify_variation.pl' $datadir'/wormbase-acedb/variation/Variation.xml' $datadir'/wormbase-acedb/variation/XML/prepped_variation.xml'


# panther
echo 'panther'
mkdir -p $datadir/panther
wget -O $datadir'/panther/RefGenomeOrthologs.tar.gz' ftp://ftp.pantherdb.org/ortholog/current_release/RefGenomeOrthologs.tar.gz
tar xzvf $datadir'/panther/RefGenomeOrthologs.tar.gz' -C $datadir'/panther'
rm -v $datadir'/panther/RefGenomeOrthologs.tar.gz'

cd $intermine'/wormmine'
pwd
../bio/scripts/project_build -b -v localhost wormmine_dump

