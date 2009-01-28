#!/usr/bin/perl -w

use strict;
use warnings;

use InterMine::PathQuery qw(AND OR);
use InterMine::WebService::Service::QueryService;
use InterMine::WebService::Service::ModelService;

my @service_args = ('http://preview.flymine.org/query/service', 'service_example');

my $query_service = new InterMine::WebService::Service::QueryService(@service_args);
my $model_service = new InterMine::WebService::Service::ModelService(@service_args);

my $path_query = new InterMine::PathQuery($model_service->get_model());

$path_query->add_view('Organism.name Organism.taxonId');
$path_query->sort_order('Organism.name');


## now constraint the genus
my $genus_c = $path_query->add_constraint('Organism.genus = "Drosophila"');
my $name_c = $path_query->add_constraint('Organism.species != "melano%"');
my $taxonid_c = $path_query->add_constraint('Organism.taxonId = 9606');

$path_query->logic(OR(AND($genus_c, $name_c), $taxonid_c));

my $drosophila_res = $query_service->get_result($path_query);
print "\n", '-' x 70, "\nOnly drosophilas:\n";
print $drosophila_res->content();

