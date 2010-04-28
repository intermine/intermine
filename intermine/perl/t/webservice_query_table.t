#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 2;

use InterMine::WebService::Service::QueryService;

my $service = new InterMine::WebService::Service::QueryService('http://www.flymine.org/query/service', 'service_tests');

my $query = q(
   <query name="" model="genomic" view="Organism.name Organism.taxonId" sortOrder="Organism.name"/>
);

my @res = $service->get_result_table($query);

ok(scalar(@res) > 1);

my $dros_taxonid = -1; # Set up a deliberate false value

for my $row_ref (@res) {
  if ($row_ref->[0] eq 'Drosophila melanogaster') {
    $dros_taxonid = $row_ref->[1];
  }
}

is(7227, $dros_taxonid, 'found Drosophila melanogaster taxon id');
