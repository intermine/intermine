#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 9;
use Test::Exception;

my $module = 'InterMine::WebService::Core::Request';
my ($rtype, $url) = ('GET', 'www.fakeurl.org');
my %params = (param1 => 'this',
	      param2 => 'that',
	      param3 => 'the other',);


use_ok($module); # Test 1

throws_ok {$module->new} qr/Not enough arguments/, # Test 2
	  'Catches poorly built requests ok';

my $req = new_ok($module => [$rtype, $url]); # Test 3

is($req->get_url, $url, 'Stores and retrieves url ok'); # Test 4
is($req->get_request_type, $rtype, 'Stores and retrieves request type ok'); # Test 5
is($req->get_content_type, 'TAB', 'Sets default content type ok'); # Test 6

$req = new_ok($module => [$rtype, $url, 'FAKETYPE']); # Test 7

is($req->get_content_type, 'FAKETYPE', # Test 8
   'Stores and retrieves content type ok');

$req->add_parameters(%params);
my %got_params = $req->get_parameters;
$params{format} = 'faketype';
is_deeply(\%got_params, \%params, # Test 9
	  'Stores and retrieves parameters ok');

