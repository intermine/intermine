#!/usr/bin/perl -w

# This sample shows how to use these modules to call the API,
# returning results as either a tab delimited list or
# formated by perl by using the result_table method
#
# This script implements the Gene => Adjacent genes (upstream and downstream) template
# url: http://www.flymine.org/release-24.0/template.do?name=Gene%20_adjacent%20genes
#
# You can use this script to find the upstream and the downstream adjacent genes
# for a particular gene.


use strict;
use warnings;

# Basic Housekeeping
# This module creates objects to pass to the service
use InterMine::PathQuery;                          
# This module configures the access to the webservice
use InterMine::WebService::Service::QueryService;  
# This module fetches the relevant data model, which we use to check our queries
use InterMine::WebService::Service::ModelService;  

my ($url, $app_name) = ('http://www.flymine.org/query/service', 'service_example');

my $query_service = InterMine::WebService::Service::QueryService->new($url, $app_name);
my $model_service = InterMine::WebService::Service::ModelService->new($url, $app_name);

my $model         = $model_service->get_model;
my $path_query    = InterMine::PathQuery->new($model);

#### Set the output columns,
# argument can be a array of paths instead, 
# or equally add_view can be called multiple times
$path_query->add_view("Gene.secondaryIdentifier Gene.downstreamIntergenicRegion.adjacentGenes.secondaryIdentifier Gene.upstreamIntergenicRegion.adjacentGenes.secondaryIdentifier");

# set the sort order, default is the first path in the view
$path_query->sort_order('Gene.secondaryIdentifier');

# now add constraints
my $gene       = 'runt';
my $constraint = $path_query->add_constraint(qq/Gene LOOKUP "$gene"/); # TODO add extra value feature
      # Other constraints are possible, including !=, CONTAINS, etc, as well as wildcards
$path_query->add_constraint(qq/Gene.downstreamIntergenicRegion.adjacentGenes != "Gene"/);
$path_query->add_constraint(qq/Gene.upstreamIntergenicRegion.adjacentGenes != "Gene"/);

# get the results as one string, containing multiple lines and tab delimited columns
# get_result takes between 1 and 3 arguments
# The query is the first (either as an InterMine::PathQuery object or as an xml string), 
#     this is the only mandatory argument
# Then optionally $start (the index of the first result you want - defaults to 0)
# Then optionally $max_count (the total number of results you want - defaults to 100)

my $res = $query_service->get_result($path_query); # This returns an HTTP::Request object 
                                                   # (see perldoc HTTP::Request)

print '-' x 70, "\n" x 2, "the upstream and the downstream adjacent genes for $gene:", 
    "\n" x 2;
print $res->content() unless $res->is_error;

my $organism = 'D. melanogaster';
$constraint->extra_value($organism);

print '-' x 70, "\n" x 2, "the upstream and the downstream adjacent genes for $gene, but only for D. Melanogaster:", 
    "\n" x 2;
$res = $query_service->get_result($path_query);
print $res->content() unless $res->is_error;
