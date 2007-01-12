#!/usr/bin/perl -w

BEGIN {
  push @INC, "$ENV{HOME}/svn/dev/bio/perl/lib";
}

use strict;

BEGIN { 
  # hard coded path at the moment - needs fixing
  $InterMine::model_file = '/tmp/genomic_model.xml';
}

# specify which classes will be used
use InterMine qw(Gene Protein Exon Transcript LocatedSequenceFeature Organism);

# find a Gene with the given ID
my $test_gene = InterMine::Gene->new(id => 122000038);
$test_gene->load();

# print its identifier
print $test_gene->identifier(), "\n";

# print the taxon id
print $test_gene->organism()->taxonId(), "\n";

# print the ids of its exons
for my $exon ($test_gene->exons()) {
  print $exon->identifier(), "\n";
}

# ... and proteins
for my $prot ($test_gene->proteins()) {
  print $prot->identifier(), "\n";
}

# find the genes that have identifier starting with "CG321", sort the
# list by their symbols
my $genes = 
  InterMine::Gene::Manager->get_genes(
                                      query =>
                                      [
                                       identifier => { like => 'CG321%' },
                                      ],
                                      sort_by => 'symbol',
                                      with_objects => 'organism'
                                     );

print "there are ", scalar(@$genes), " genes with identifier starting with CG321\n";

# print identifier and taxon IDs
for my $gene (@$genes) {
  print $gene->identifier, "\n";
  if (defined $gene->organism) {
    print $gene->organism->taxonId(), "\n";
  }
}

# find the genes that have identifier starting with CG321, sort by
# their symbols and return an object that will iterate over them
my $iterator = 
  InterMine::Gene::Manager->get_genes_iterator(
                                               query =>
                                               [
                                                identifier => { like => 'CG321%' },
                                               ],
                                               sort_by => 'symbol',
                                               with_objects => 'organism'
                                              );

my $total_len = 0;

# iterate over the genes, print the taxon id and add up the lengths
while (my $gene = $iterator->next) {
  print $gene->identifier, "\n";
  if (defined $gene->organism) {
    print $gene->organism->taxonId(), "\n";
  }

  my $len = $gene->length();
  if (defined $len) {
    $total_len += $len;
  }
}


print "total gene length: ", $total_len, "\n";

my $gene_count = 
  InterMine::Gene::Manager->get_genes_count(
                                             query =>
                                             [
                                              identifier => { like => 'CG321%' },
                                             ]
                                            );

print "average length of genes starting with 'CG321': ", $total_len / $gene_count, "\n";
