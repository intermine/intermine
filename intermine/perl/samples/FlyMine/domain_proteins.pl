#!/usr/bin/perl -w

# This sample shows how to use these modules to call the API,
# returning results as either a tab delimited list or
# formated by perl by using the result_table method
#
# This script implements the Protein domain => Proteins from a specific organism template
# url: http://www.flymine.org/release-24.0/template.do?name=Domain_Proteins
#
# You can use this script to show the proteins (from a specific organism) 
# which have a particular domain.

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
$path_query->add_view("Protein.primaryIdentifier Protein.primaryAccession Protein.genes.primaryIdentifier Protein.genes.symbol Protein.proteinDomains.name Protein.proteinDomains.primaryIdentifier Protein.proteinDomains.type");

# set the sort order, default is the first path in the view
$path_query->sort_order('Protein.genes.symbol');

# now add constraints
my $organism    = 'Drosophila melan*';
$path_query->add_constraint(qq/Protein.organism.name = "$organism"/);
      # Other constraints are possible, including !=, CONTAINS, etc, as well as wildcards
my $protein_domain = 'Homeobox';
$path_query->add_constraint(qq/Protein.proteinDomains.name = "$protein_domain"/);

# get the results as one string, containing multiple lines and tab delimited columns
# get_result takes between 1 and 3 arguments
# The query is the first (either as an InterMine::PathQuery object or as an xml string), 
#     this is the only mandatory argument
# Then optionally $start (the index of the first result you want - defaults to 0)
# Then optionally $max_count (the total number of results you want - defaults to 100)

my $res = $query_service->get_result($path_query); # This returns an HTTP::Request object 
                                                   # (see perldoc HTTP::Request)

print '-' x 70, "\n" x 2, "Proteins from $organism in the $protein_domain protein domain:", 
    "\n" x 2;
print $res->content() unless $res->is_error;
