#!/usr/bin/perl -w

use strict;
use warnings;

use InterMine::WebService::Service::QueryService;

my $service = new InterMine::WebService::Service::QueryService('http://www.flymine.org/query/service', 'service_example');

my $query = q(
   <query name="" model="genomic" view="Organism.name Organism.taxonId" sortOrder="Organism.name"/>
);

my $count = $service->get_count($query);

print "result count: $count\n";

my $res = $service->get_result($query);

print $res->content();

