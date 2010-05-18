#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 11;
use Test::Exception;
use Test::MockObject;
use URI;

use InterMine::WebService::Core::Request;

### Setting up

my $module = 'InterMine::WebService::Core::Service';
my ($url, $app) = ('http://www.fakeurl.org', 'fake_app');
my %params = ('test parameter 1' => 'Some value',
	      'test parameter 2' => 'Another value',
	      'test parameter 3' => '!$=&<>'); # illegal characters

my $fakeLWP = Test::MockObject->new();
$fakeLWP->fake_module('LWP::UserAgent',
		   new => sub {return $fakeLWP},
		   );
$fakeLWP->mock(env_proxy => sub {})
        ->mock(agent => sub {})
        ->mock(get => sub {my ($self, $url) = @_; return $url})
    ->mock(post => sub{my ($self, $url, $params) = @_; return($url, $params) });
		   
### Tests

use_ok($module); # Test 1
throws_ok {$module->new()} qr/No url provided/, 'Lack of url caught ok'; # Test 2
throws_ok {$module->new('badurl.org')} qr/URL must be absolute/, 'Relative url caught ok'; # Test 3

my $service = new_ok($module => [$url, $app]);
is($service->get_url, $url.'/', 'Can add a trailing / to the url'); # Test 4

$service = $module->new($url.'/', $app); # Test 5
is($service->get_url, $url.'/', "Doesn't add trailing /s to the url unnecessarily"); # Test 6

$service->{_SERVICE_RELATIVE_URL} = 'fake/service';
is($service->get_url, $url.'/fake/service', "can handle relative paths"); # Test 7

my $request = InterMine::WebService::Core::Request->new('GET', 
                                                        $service->get_url,
                                                        'TAB',
    );

is($service->execute_request($request), # Test 8
   'http://www.fakeurl.org/fake/service?format=tab', 
   'Execute a basic get request'); 

$request->add_parameters(%params);

my $expected_url = URI->new('http://www.fakeurl.org/fake/service?test+parameter+1=Some+value&test+parameter+2=Another+value&format=tab&test+parameter+3=!%24%3D%26%3C%3E');
ok($expected_url->eq($service->execute_request($request)), # Test 9
   'Execute a more complex request');


$request = InterMine::WebService::Core::Request->new('POST', 
                                                        $service->get_url,
                                                        'TAB',
    );
$request->add_parameters(%params);
my ($got_url, $got_params) = $service->execute_request($request);
is($got_url, 'http://www.fakeurl.org/fake/service', 'Can post to the right url'); # Test 10
$params{format} = 'tab';
is_deeply($got_params, \%params, 'Can pass params along in postdata'); # Test 11
