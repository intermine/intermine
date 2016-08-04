#!/bin/bash

#Paulo Nuin Jan 2015, modified Feb 15 - Aug 2016

# TODO: set release version as a script argument
# TODO: add mapping/property files to repo and copy process here

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

sourcedir='/mnt/data/acedb_dumps/'$wbrel'' # <---- XML dump location

#################### Main dirs ##################
#                                               #
#  datadir - main data directory                # 
#  acexmldir - subdir for AceDB XML files       #
#  pp - pre-processing dir with perl and bash   #
#                                               #
#################### Species ####################
intermine='/mnt/data/intermine'
# intermine='/Users/nuin/intermine_work/new/intermine' #local test
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
    # wget -O "$spe"."${species["$spe"]}"."$wbrel".protein.fa.gz "ftp://206.108.120.212/pub/wormbase/releases/"$wbrel"/species/"$spe"/"${species["$spe"]}"/"$spe"."${species["$spe"]}"."$wbrel".protein.fa.gz"
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
  wget -O $datadir/go/gene_ontology."$wbrel".obo "ftp://206.108.120.212/pub/wormbase/releases/"$wbrel"/ONTOLOGY/gene_ontology."$wbrel".obo"
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

#echo 'anatomy_term'
#mkdir -vp $datadir/wormbase-acedb/anatomy_term/XML
#mkdir -vp $datadir/wormbase-acedb/anatomy_term/mapping
#cp -v $sourcedir/Anatomy_term.xml $acexmldir/anatomy_term/Anatomy_term.xml
#cp -v /mnt/data/properties/anatomy_term_mapping.properties $datadir/wormbase-acedb/anatomy_term/mapping/
# perl $pp/wb-acedb/anatomy_term//prep_anatomy_term.pl $datadir/wormbase-acedb/anatomy_term/Anatomy_term.xml $datadir/wormbase-acedb/anatomy_term/XML/Anatomy_term_pre$

# echo 'cds'
#mkdir -vp $datadir/wormbase-acedb/cds/XML
#mkdir -vp $datadir/wormbase-acedb/cds/mapping
#cp -v $sourcedir/CDS.xml $acexmldir/cds/CDS.xml
#cp -v /mnt/data/properties/cds_mapping.properties $datadir/wormbase-acedb/cds/mapping/
#perl /mnt/data/intermine/testlab/perl/purify_xace/purify_xace.pl $datadir/wormbase-acedb/cds/CDS.xml $datadir/wormbase-acedb/cds/purified_CDS.xml
#perl $pp/wb-acedb/cds/prep_wb-acedb-cds.pl $datadir/wormbase-acedb/cds/purified_CDS.xml $datadir/wormbase-acedb/cds/XML/prepped_CDS.xml
#rm $datadir/wormbase-acedb/cds/purified_CDS.xml


#echo 'expr_cluster'
#mkdir -vp $datadir/wormbase-acedb/expr_cluster/XML
#mkdir -vp $datadir/wormbase-acedb/expr_cluster/mapping
#cp $sourcedir/Expression_cluster.xml $acexmldir/expr_cluster/Expression_cluster.xml
#cp /mnt/data/properties/expr_cluster_mapping.properties $datadir/wormbase-acedb/expr_cluster/mapping/
#cp /mnt/data/properties/anatomy_term_mapping.properties $datadir/wormbase-acedb/anatomy_term/mapping
#perl /mnt/data/intermine/testlab/perl/purify_xace/purify_xace.pl $datadir/wormbase-acedb/expr_cluster/Expression_cluster.xml $datadir/wormbase-acedb/expr_cluster/X$


#echo 'expr_pattern'
#mkdir -vp $datadir/wormbase-acedb/expr_pattern/XML
#mkdir -vp $datadir/wormbase-acedb/expr_pattern/mapping
#cp $sourcedir/Expr_pattern.xml $acexmldir/expr_pattern/Expr_pattern.xml
#cp /mnt/data/properties/expr_pattern_mapping.properties $datadir/wormbase-acedb/expr_pattern/mapping/
#perl /mnt/data/intermine/testlab/perl/preprocess/wb-acedb/expr_pattern/prep_expr_pattern.pl $datadir/wormbase-acedb/expr_pattern/Expr_pattern.xml $datadir/wormbase$


#echo 'gene'
#mkdir -vp $datadir/wormbase-acedb/gene/XML
#mkdir -vp $datadir/wormbase-acedb/gene/mapping
#cp $sourcedir/Gene.xml $acexmldir/gene/Gene.xml
#cp /mnt/data/properties/wormbase-acedb-gene.properties $datadir/wormbase-acedb/gene/mapping/
#perl /mnt/data/intermine/testlab/perl/purify_xace/purify_xace.pl $datadir/wormbase-acedb/gene/Gene.xml $datadir/wormbase-acedb/gene/purified_gene.xml
#perl $pp/wb-acedb/gene/prep_wb-acedb-gene.pl $datadir/wormbase-acedb/gene/purified_gene.xml $datadir/wormbase-acedb/gene/XML/prepped_gene.xml
#rm $datadir/wormbase-acedb/gene/purified_gene.xml

#echo 'life_stage'
#mkdir -vp $datadir/wormbase-acedb/life_stage/XML
#mkdir -vp $datadir/wormbase-acedb/life_stage/mapping
#cp $sourcedir/Life_stage.xml $acexmldir/life_stage/Life_stage.xml
#cp /mnt/data/properties/life_stage_mapping.properties $datadir/wormbase-acedb/life_stage/mapping/
# perl /mnt/data/intermine/testlab/perl/purify_xace/purify_xace.pl $datadir/wormbase-acedb/life_stage/Life_stage.xml $datadir/wormbase-acedb/life_stage/XML/purified_l$


#echo 'phenotype'
#mkdir -vp $datadir/wormbase-acedb/phenotype/XML
#mkdir -vp $datadir/wormbase-acedb/phenotype/mapping
#cp $sourcedir/Phenotype.xml $acexmldir/phenotype/Phenotype.xml
#cp /mnt/data/properties/phenotype_mapping.properties $datadir/wormbase-acedb/phenotype/mapping
#perl /mnt/data/intermine/testlab/perl/purify_xace/purify_xace.pl $datadir/wormbase-acedb/phenotype/Phenotype.xml $datadir/wormbase-acedb/phenotype/XML/purified_phe$


#echo 'protein'
#mkdir -vp $datadir/wormbase-acedb/protein/XML
#mkdir -vp $datadir/wormbase-acedb/protein/mapping
#cp $sourcedir/Protein.xml $acexmldir/protein/Protein.xml
#cp /mnt/data/properties/protein_mapping.properties $datadir/wormbase-acedb/protein/mapping
#perl /mnt/data/intermine/testlab/perl/preprocess/wb-acedb/protein/prep_wb-acedb-protein.pl $datadir/wormbase-acedb/protein/Protein.xml $datadir/wormbase-acedb/prot$
#perl /mnt/data/intermine/testlab/perl/preprocess/wb-acedb/protein/purge_protein.pl $datadir/wormbase-acedb/protein/prepped_protein.xml $datadir/wormbase-acedb/prot$
#rm $datadir/wormbase-acedb/protein/prepped_protein.xml


#echo 'species'
#mkdir -vp $datadir/wormbase-acedb/species/XML
#mkdir -vp $datadir/wormbase-acedb/species/mapping
#cp $sourcedir/Species.xml $acexmldir/species/Species.xml
#cp /mnt/data/properties/species_mapping.properties $datadir/wormbase-acedb/species/mapping


#echo 'transcript'
#mkdir -vp $datadir/wormbase-acedb/transcript/XML
#mkdir -vp $datadir/wormbase-acedb/transcript/mapping
#cp $sourcedir/Transcript.xml $acexmldir/transcript/Transcript.xml
#cp /mnt/data/properties/transcript_mapping.properties $datadir/wormbase-acedb/transcript/mapping
#perl /mnt/data/intermine/testlab/perl/preprocess/wb-acedb/transcript/prep_wb-acedb-transcript.pl $datadir/wormbase-acedb/transcript/Transcript.xml $datadir/wormbas$


#echo 'variation'
#mkdir -vp $datadir/wormbase-acedb/variation/XML
#mkdir -vp $datadir/wormbase-acedb/variation/mapping
#cp $sourcedir/Variation.xml $acexmldir/variation/Variation.xml
#cp /mnt/data/properties/variation_mapping.properties $datadir/wormbase-acedb/variation/mapping
#perl /mnt/data/intermine/testlab/perl/preprocess/wb-acedb/variation/purify_variation.pl $datadir/wormbase-acedb/variation/Variation.xml $datadir/wormbase-acedb/var$


#cd /mnt/data/intermine/wormmine/
#./xx





# gffurl="ftp://206.108.125.180/pub/wormbase/releases/"$wbrel"/species/c_elegans/PRJNA13758/c_elegans.PRJNA13758."$wbrel".annotations.gff3.gz"

# declare -A species=(["a_ceylanicum"]="PRJNA231479" ["a_suum"]="PRJNA62057"
#  ["b_xylophilus"]="PRJEA64437"  ["c_sp5"]="PRJNA194557"  ["d_immitis"]="PRJEB1797" ["h_bacteriophora"]="PRJNA13977"
#  ["h_contortus"]="PRJEB506" ["l_loa"]="PRJNA60051" ["m_hapla"]="PRJNA29083" ["m_incognita"]="PRJEA28837"
#  ["n_americanus"]="PRJNA72135"  ["p_exspectatus"]="PRJEB6009"
#  ["t_spiralis"]="PRJNA12603" ["t_suis"]="PRJNA208415")