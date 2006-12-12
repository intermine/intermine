#!/usr/bin/perl -w

BEGIN {
  push @INC, "$ENV{HOME}/svn/dev/bio/perl/lib";
}

use strict;

BEGIN { 
  $InterMine::model_file = '/home/kmr/svn/dev/flymine/dbmodel/build/model/genomic_model.xml'; 
}

use InterMine qw(Gene);
use InterMine qw(Organism);

my $g = InterMine::Gene->new(id => 127000061);
$g->load();

print $g->identifier(), "\n";
print $g->organism()->taxonId(), "\n";

my $genes = 
  InterMine::Gene::Manager->get_genes(
                                      query =>
                                      [
                                       identifier => { like => 'CG1111%' },
                                      ]
                                     );

for my $gene (@$genes) {
  print $gene->identifier, "\n";
}
