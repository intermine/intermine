#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 11;
use Test::Exception;
use Test::MockObject::Extends;

### Setting up

my $module = 'InterMine::WebService::Service::ModelService';
my $url = 'http://www.fakeurl.org/query/service';
my @methods = (qw/new get_model_xml get_model/);

my $test_model = '../objectstore/model/testmodel/testmodel_model.xml';
my $test_xml;
{
    local $/;
    open(MODEL, '<', $test_model) or die "Can't open model\n";
    $test_xml = <MODEL>;
    close(MODEL) or die "Can't close model";
}

my $fake_res = Test::MockObject::Extends->new;
$fake_res->set_false('is_error')
    ->mock(content => sub {return $test_xml});

### Tests

use_ok($module);
can_ok($module, @methods);

my $service = new_ok($module => [$url]);
isa_ok($service, 'InterMine::WebService::Core::Service', 'Inherits ok');

is($service->get_url, $url.'/model', 'Sets the url ok');

$service = Test::MockObject::Extends->new($service);

### Test get_model_xml
### execute_request is tested in 2webservice_core_service.t
$service->mock(execute_request => sub {my ($self, $req) = @_; return $req});
my $ret = $service->get_model_xml;

isa_ok($ret, 'InterMine::WebService::Core::Request', 'Model request');
is($ret->get_url, $service->get_url, 'Passes url ok');
is_deeply({$ret->get_parameters}, {format => 'text'}, 'Sets parameters ok');

### Test get_model
$service->mock(get_model_xml => sub {return $fake_res});
my $model = $service->get_model();

ok(defined $model, 'Returns something from get_model ok');
isa_ok($model, 'InterMine::Model', "Returned model");
ok($model->get_all_classdescriptors > 5, "The model isn't empty");



