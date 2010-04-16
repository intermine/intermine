#!/usr/bin/perl -w

# This sample shows how to use these modules to call the API,
# returning results as either a tab delimited list or
# formated by perl by using the result_table method
#
# In addition this example shows how to specify the search logic
# and how to use wildcards in searchterms

use strict;
use warnings;

# Basic Housekeeping
use InterMine::PathQuery qw(AND OR);             # This module creates objects to pass to the service, 
                                                 # Here we import the AND and OR functions 
use InterMine::WebService::Service::QueryService;# This module configures the access to the webservice
use InterMine::WebService::Service::ModelService;# This module fetches the relevant data model, 
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

# Determine which column will be used to sort the results
$path_query->sort_order('Organism.name'); # not strictly necessary, since this defaults 
                                          # to the first named view anyway

#### Now add constraints to the search
# Constrain the genus 
my $genus_c    = $path_query->add_constraint('Organism.genus = "Drosophila"'); 
                                                             # is Drosophila

# Constrain the species - note the use of wildcards (can be used at the beginning or end)
my $name_c     = $path_query->add_constraint('Organism.species != "melano*"'); 
                                                               # doesn't begin with 'melano'

# Constrain the taxonomic id - note that numbers do not need enclosing quotes
my $taxonid_c  = $path_query->add_constraint('Organism.taxonId = 9606');
                                                               # is precisely 9606 

# Specify the way these constraints should work, 
$path_query->logic( OR(                     
                     AND($genus_c, $name_c), # either it is a Drosophila but not beginning with melano*
                     $taxonid_c,             # or it has the taxonomic id of precisely 9606
                    )
                  );

my $drosophila_res = $query_service->get_result($path_query);  # This returns an HTTP::Request object 
                                                               # (see perldoc HTTP::Request)
print "\n", '-' x 70, "\nOnly drosophilas:\n";
print $drosophila_res->content() unless $res->is_error;

