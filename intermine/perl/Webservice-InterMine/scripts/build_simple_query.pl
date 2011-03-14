#/usr/bin/perl

use strict;
use warnings;

use lib 'lib';

use Webservice::InterMine::Simple;

my $url = 'http://localhost:8080/intermine-test/service';

my $service = Webservice::InterMine::Simple->get_service($url);
my $query = $service->new_query(model => "testmodel");

$query->add_view(qw/Employee.name Employee.age/);
$query->add_constraint(path => "Employee.age", op => '>', value => 35);
$query->add_constraint(path => "Employee.age", op => '!=', value => 55);

print $query->results(as => "string");

print $query->results(as => "string", addheader => 1, size => 3);
