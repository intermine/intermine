#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 2;

use InterMine::Util qw(get_property_value);

my $prop_file = 't/data/test.properties';


# test get_property_value()

my $prop_user = get_property_value('db.production.datasource.user', $prop_file);
my $prop_pass = get_property_value('db.production.datasource.password', $prop_file);

ok($prop_user eq 'some_user');
ok($prop_pass eq 'some_password');


# test get_property_filenames()

