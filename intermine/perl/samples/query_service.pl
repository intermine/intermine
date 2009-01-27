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

# set the output columns, argument can be a array of paths instead
$path_query->add_view('Organism.name Organism.taxonId');

# set the sort order, default is the first path in the view
$path_query->sort_order('Organism.taxonId');


# get the results as one string, containing multiple lines and tab delimited
# columns
my $res = $query_service->get_result($path_query);
print "All organisms:\n";
print $res->content();


# now constrain the genus
$path_query->add_constraint('Organism.genus = "Drosophila"');


# print the result table again - will be smaller
my $drosophila_res = $query_service->get_result($path_query);
print "\n", '-' x 70, "\nOnly Drosophilas:\n";
print $drosophila_res->content();


# get the results as a table instead
print "\n", '-' x 70, "\nDrosophila results as a table:\n";
my @res = $query_service->get_result_table($path_query);
for my $row_ref (@res) {
  my @row = @{$row_ref};

  print join (':', @row), "\n";
}
