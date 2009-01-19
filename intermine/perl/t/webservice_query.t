#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 4;

use InterMine::WebService::Service::QueryService;

my $service = new InterMine::WebService::Service::QueryService('http://www.flymine.org/query/service', 'service_tests');

my $query = q(
   <query name="" model="genomic" view="Organism.name Organism.taxonId" sortOrder="Organism.name"/>
);

my $res = $service->get_result($query);

ok(defined $res);
ok($res->is_success);

my $content = $res->content();
ok($content =~ /Drosophila/);

my @lines = $content =~ /^([^\n]*)$/gm;
ok(scalar(@lines) > 5);

