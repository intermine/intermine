#!/usr/bin/perl -w

BEGIN {
  push @INC, "$ENV{HOME}/svn/dev/bio/perl/lib";
}

use strict;

BEGIN { 
  $InterMine::model_file = '/home/kmr/svn/dev/flymine/dbmodel/build/model/genomic_model.xml'; 
}

use InterMine qw(Gene);
use InterMine qw(Exon);
use InterMine qw(LocatedSequenceFeature);
use InterMine qw(Organism);

my $g = InterMine::Gene->new(id => 122000038);
$g->load();

print $g->identifier(), "\n";
print $g->organism()->taxonId(), "\n";
for my $exon ($g->exons()) {
  print $exon->identifier(), "\n";
}

# my $genes = 
#   InterMine::Gene::Manager->get_genes(
#                                       query =>
#                                       [
#                                        identifier => { like => 'C%' },
#                                       ],
#                                       sort_by => 'symbol',
#                                       with_objects => 'organism'
#                                      );

# print scalar(@$genes), "\n";

# for my $gene (@$genes) {
#   $gene->identifier, "\n";
#   if (defined $gene->organism) {
#     $gene->organism->taxonId(), "\n";
#   }
# }

# my $iterator = 
# InterMine::Gene::Manager->get_genes_iterator(
#                                              query =>
#                                              [
#                                               identifier => { like => 'C%' },
#                                              ],
#                                              sort_by => 'symbol',
#                                              with_objects => 'organism'
#                                             );

# while (my $gene = $iterator->next) {
#   $gene->identifier, "\n";
#   if (defined $gene->organism) {
#     $gene->organism->taxonId(), "\n";
#   }
# }

my $iterator = 
  InterMine::LocatedSequenceFeature::Manager->get_locatedsequencefeatures_iterator();

my $i = 0;
my $total_len = 0;

while (my $lsf = $iterator->next) {
#  print $lsf->identifier, "\n";
  my $len = $lsf->length();
  if (defined $len) {
    $total_len += $len;
  }

  last;
}


print $total_len, "\n";
