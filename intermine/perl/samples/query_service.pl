#!/usr/bin/perl -w

use strict;
use warnings;

use InterMine::PathQuery;
use InterMine::WebService::Service::QueryService;
use InterMine::WebService::Service::ModelService;

my @service_args = ('http://www.flymine.org/query/service', 'service_example');

my $query_service = new InterMine::WebService::Service::QueryService(@service_args);
my $model_service = new InterMine::WebService::Service::ModelService(@service_args);

my $path_query = new InterMine::PathQuery($model_service->get_model());

$path_query->add_view("Organism.name Organism.taxonId");
$path_query->sort_order("Organism.name");

warn 'xml: ', $path_query->to_xml_string(), "\n";

my $count = $query_service->get_count($path_query);

print "result count: $count\n";

my $res = $query_service->get_result($path_query);

print $res->content();

