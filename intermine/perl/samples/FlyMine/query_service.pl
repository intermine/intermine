#!/usr/bin/perl -w

# This sample shows how to use these modules to call the API,
# returning results as either a tab delimited list or
# formated by perl by using the result_table method

use strict;
use warnings;

# Basic Housekeeping
use InterMine::PathQuery;                          # This module creates objects to pass to the service
use InterMine::WebService::Service::QueryService;  # This module configures the access to the webservice
use InterMine::WebService::Service::ModelService;  # This module fetches the relevant data model, 
                                                   # which we use to check that our queries are legal.

my ($url, $app_name) = ('http://preview.flymine.org/query/service', 'service_example');

my $query_service = InterMine::WebService::Service::QueryService->new($url, $app_name);
my $model_service = InterMine::WebService::Service::ModelService->new($url, $app_name);

my $model         = $model_service->get_model;
my $path_query    = InterMine::PathQuery->new($model);

#### Set the output columns,
# argument can be a array of paths instead, 
# or equally add_view can be called multiple times
$path_query->add_view('Organism.name Organism.taxonId');

# set the sort order, default is the first path in the view
$path_query->sort_order('Organism.taxonId');

# get the results as one string, containing multiple lines and tab delimited columns
# get_result takes between 1 and 3 arguments
# The query is the first (either as an InterMine::PathQuery object or as an xml string), 
#     this is the only mandatory argument
# Then optionally $start (the index of the first result you want, useful if paging - defaults to 0)
# Then optionally $max_count (the total number of results you want - defaults to 100)
my $res = $query_service->get_result($path_query);# This returns an HTTP::Request object 
                                                  # (see perldoc HTTP::Request

print "All organisms:\n";
print $res->content() unless $res->is_error;

# now constrain the genus
$path_query->add_constraint('Organism.genus = "Drosophila"');

# print the result table again - will be smaller
my $drosophila_res = $query_service->get_result($path_query); # This returns an HTTP::Request object 
                                                              # (see perldoc HTTP::Request)
print "\n", '-' x 70, "\nOnly Drosophilas:\n";
print $drosophila_res->content() unless $drosophila_res->is_error;

# get the results as a table instead
print "\n", '-' x 70, "\nDrosophila results as a table:\n";
my @res = $query_service->get_result_table($path_query); # This returns a reference to an array of arrays
for my $row_ref (@res) {
  my @row = @{$row_ref};

  print join (':', @row), "\n";
}
