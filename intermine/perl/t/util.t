#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 4;

use InterMine::Util qw(get_property_value get_java_type_name);

my $prop_file = 't/data/test.properties';


# test get_property_value()

my $prop_user = get_property_value('db.production.datasource.user', $prop_file);
my $prop_pass = get_property_value('db.production.datasource.password', $prop_file);

ok($prop_user eq 'some_user');
ok($prop_pass eq 'some_password');

ok(get_java_type_name('gene') eq 'Gene');
ok(get_java_type_name('five_prime_UTR') eq 'FivePrimeUTR');

# test get_property_filenames()

