#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 6;

use InterMine::WebService::Service::ModelService;

my $service = new InterMine::WebService::Service::ModelService('http://www.flymine.org/query/service', 'service_tests');

my $res = $service->get_model_xml();

ok(defined $res);
ok($res->is_success);


my $content = $res->content();
ok($content =~ /BioEntity/);

my $model = $service->get_model();

ok(defined $model);
ok(ref $model eq 'InterMine::Model');

# check that we have some classes
ok(scalar($model->get_all_classdescriptors()) > 5);

