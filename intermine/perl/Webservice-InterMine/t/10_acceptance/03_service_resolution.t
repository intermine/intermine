#!/usr/bin/perl

use strict;
use warnings;
use Test::More;
use Test::Exception;

use Webservice::InterMine;
require Webservice::InterMine::ServiceResolver;

my $do_live_tests = $ENV{RELEASE_TESTING};

unless ($do_live_tests) {
    plan( skip_all => "Acceptance tests for release testing only" );
} else {
    plan( tests => 7 );
}

my $service;

lives_ok {$service = Webservice::InterMine->get_service('flymine');}
    "Fetches a service by name";

ok($service->version > 1, "The service seems valid");

my $autoloaded_service;

lives_ok{$autoloaded_service = Webservice::InterMine->get_flymine;}
    "Fetches a service by name using autoloading";

ok($autoloaded_service->version > 1, "The service seems valid");

is(Webservice::InterMine::ServiceResolver->get_fetch_count, 1,
    "Caches the data properly");

throws_ok {Webservice::InterMine->get_foomine} 
    qr/Could not resolve foomine/,
    "Dies on bad mine name";

throws_ok {Webservice::InterMine->sadkfj} 
    qr/no method named sadkfj/,
    "Dies on bad method name";

