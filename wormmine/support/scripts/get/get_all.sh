B1;2c#!/bin/bash

RELEASE=$1

./get_anatomy_ontology.pl --release $RELEASE
./get_gene_ontology.pl --release $RELEASE
./get_interpro.pl  --release $RELEASE
./get_uniprot.pl  --release $RELEASE
./get_phenotype_ontology.pl  --release $RELEASE

./get_wormbase_identifiers.pl  --release $RELEASE

./get_genomic_fasta_and_annotations.pl  --release $RELEASE