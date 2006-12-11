#!/usr/bin/perl -w

BEGIN {
  push @INC, "$ENV{HOME}/svn/dev/bio/perl/lib";
}

use strict;

BEGIN { $InterMine::model_file = '/home/kmr/svn/dev/flymine/dbmodel/build/model/genomic_model.xml'; }

use InterMine qw(Gene);

my $g = InterMine::Gene->new(id => 125000479);
$g->load();

use Data::Dumper;

#print Dumper($g);
#print "\n";

print $g->organismDbId(), "\n";

1;
