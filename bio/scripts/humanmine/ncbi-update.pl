#!/usr/bin/perl

use strict;
use warnings;
use Getopt::Std;
require LWP::UserAgent;
use Tie::File;
use feature ':5.12';

# Print unicode to standard out
binmode(STDOUT, 'utf8');
# Silence warnings when printing null fields
no warnings ('uninitialized');


########################################
################# HGNC #################
########################################

my $hgnc_file = "/micklem/data/human/hgnc/current/hgnc_complete_set.txt";

open(HGNC_FILE, "< $hgnc_file") || die "cannot open $hgnc_file: $!\n";

print "reading in HGNC file from $hgnc_file\n";

my %identifier_hash;

while (<HGNC_FILE>) {
  chomp;

  my @line = split(/\t/,$_);
  my $symbol = $line[1]; 
  my $ncbi = $line[18];
  my $ensembl = $line[19];

  if (!$ncbi eq '' && !$ensembl eq '') {
    $identifier_hash{$ncbi} = $ensembl; 
  }
}

print "Done reading HGNC file\n";

########################################
########################################
########################################

my $ncbi_file = "/micklem/data/ncbi/current/All_Data.gene_info";

my $human_taxon = "9606";

print "Reading in NCBI file from $ncbi_file\n";

open(NCBI_FILE, "< $ncbi_file") || die "cannot open $ncbi_file: $!\n";

            #String entrez = line[1].trim();
            #String defaultSymbol = line[2].trim();
            #String locusTag = line[3].trim();
            #String synonyms = line[4].trim();
            #String xrefs = line[5].trim(); // db Identifiers
            #String mapLocation = line[7].trim();
            #String defaultName = line[8].trim();
            #String geneType = line[9].trim();
            #String officialSymbol = line[10].trim();
            #String officialName = line[11].trim();

# 9606    34      ACADM   -       ACAD1|MCAD|MCADH        MIM:607008|HGNC:HGNC:89|Ensembl:ENSG00000117054|HPRD:08447|Vega:OTTHUMG00000009784      1       1p31    acyl-CoA dehydrogenase, C-4 to C-12 straight chain      protein-coding  ACADM   acyl-CoA dehydrogenase, C-4 to C-12 straight chain      O       acyl-Coenzyme A dehydrogenase, C-4 to C-12 straight chain       20151113

my $ensembl_prefix = "Ensembl:";
my $output_file = "/tmp/renamed-ncbi.txt";
open(my $OUTPUT_FILE, ">", $output_file) or die $!;
my $i = 0;

while (<NCBI_FILE>) {
  chomp;

  my @line = split(/\t/,$_);
  my $taxon = $line[0];
  my $ncbi_identifier = $line[1];
  my $xrefs = $line[5];
  my $have_printed = 0;

  # only interested in human genes
  if (index($taxon, "#") != -1 || $taxon != $human_taxon) {
    print $OUTPUT_FILE "$_\n";
    next;
  }
  
  # we have an ensembl identifier for this NCBI id
  if (exists $identifier_hash{$ncbi_identifier}) {
    my @xrefs_array = split(/"|"/, $xrefs);
    my $has_ensembl = 0;

    # look for ensembl in xrefs
    foreach my $xref(@xrefs_array) {
      # check for ensembl XREF
      if (index($xref, $ensembl_prefix) != -1) {
        $has_ensembl = 1;
      }
    }

    # we do NOT have an ensembl value for this gene in the NCBI file
    # but we DID have an ensembl id for this gene in the HGNC file
    # append the ensembl value to this entry in file
    if (!$has_ensembl) {
      my $append_text = $xrefs . "|" . $ensembl_prefix . $identifier_hash{$ncbi_identifier};
      s/"$xrefs"/$append_text/;
      print $OUTPUT_FILE "$_\n";
      $have_printed = 1;  
      $i++;
    }
  } 
  if (!$have_printed) {
    print $OUTPUT_FILE "$_\n";
  }
}

print "Wrote to $output_file. added $i Ensembl gene identifiers \n";

rename $output_file, $ncbi_file;

exit(1);
