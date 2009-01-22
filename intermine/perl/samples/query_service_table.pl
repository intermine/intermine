#!/usr/bin/perl -w

use strict;
use warnings;

use InterMine::PathQuery;
use InterMine::WebService::Service::QueryService;
use InterMine::WebService::Service::ModelService;

my @service_args = ('http://preview.flymine.org/query/service', 'service_example');

my $query_service = new InterMine::WebService::Service::QueryService(@service_args);
my $model_service = new InterMine::WebService::Service::ModelService(@service_args);

my $path_query = new InterMine::PathQuery($model_service->get_model());

$path_query->add_view('Organism.name Organism.taxonId');
$path_query->sort_order('Organism.name');


## print the result table
my @res = $query_service->get_result_table($path_query);
print "All organisms:\n";
for my $row_ref (@res) {
  my @row = @{$row_ref};

  print join (':', @row), "\n";
}
