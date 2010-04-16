#!/usr/bin/perl

# This example finds all genes in a specified organism with a particular protein domain.

# Note the use of wildcards in search terms and the ability to specify the logic of the constraints

use strict;
use warnings;

# Basic housekeeping
use InterMine::PathQuery qw(AND OR);
use InterMine::WebService::Service::QueryService;
use InterMine::WebService::Service::ModelService;

my $url = 'http://intermine.modencode.org/query/service/';
my $app_name = 'PerlAPI';

my $query_service = new InterMine::WebService::Service::QueryService($url, $app_name);
my $model_service = new InterMine::WebService::Service::ModelService($url, $app_name);

my $path_query = new InterMine::PathQuery($model_service->get_model());

#View the gene primary Identifier
$path_query->add_view('Gene.primaryIdentifier');

#View the gene symbol
$path_query->add_view('Gene.symbol');

#View the protein domains of the proteins of the gene
$path_query->add_view('Gene.proteins.proteinDomains.name');

#View the organism
$path_query->add_view('Gene.organism.name');

#Sort the gene symbol
$path_query->sort_order('Gene.symbol');

## now constrain by protein domain and organism
my $domain_c   = $path_query->add_constraint('Gene.proteins.proteinDomains.name = "Cold shock protein"');
 # You can use * as a wildcard at the beginning or end of a searchterm
my $organism_c = $path_query->add_constraint('Gene.organism.species = "melano*"');

$path_query->logic(AND($organism_c,$domain_c)); # Logic can be specified as either AND or OR, with nesting

my $first_result_to_return  = 0;  # We begin at 0
my $no_of_results_to_return = 20; # Just display the first 20 results

my $res = $query_service->get_result($path_query, 
				     $first_result_to_return, # Optional - defaults to 0
				     $no_of_results_to_return,# Optional - defaults to 100
    );

print '-' x 70, "\n" x 2, "The first 20 Cold shock protein Genes in Drosophila melanogaster", "\n" x 2;

print $res->content unless $res->is_error;

