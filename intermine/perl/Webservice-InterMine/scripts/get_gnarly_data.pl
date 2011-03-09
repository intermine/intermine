#!/usr/bin/perl

use strict;
use warnings;

use Webservice::InterMine "http://localhost:8080/intermine-test/service";

my $q = Webservice::InterMine->new_query();

my @views = qw/
    Employee.name
    Employee.age
    Employee.fullTime
    Employee.department.name
    Employee.department.manager.name
    Employee.department.manager.title
    Employee.department.manager.seniority
    Employee.department.employees.name
    Employee.department.employees.address.address
    Employee.department.company.name
    Employee.department.company.vatNumber
    Employee.department.company.contractors.name
    Employee.department.company.contractors.businessAddress.address
    Employee.department.company.secretarys.name
    Employee.department.company.address.address
    /;

$q->add_view(@views);
$q->add_outer_join("Employee.department");
$q->add_outer_join("Employee.department.manager");
$q->add_outer_join("Employee.department.employees");
$q->add_outer_join("Employee.department.company");
$q->add_outer_join("Employee.department.company.contractors");
$q->add_outer_join("Employee.department.company.secretarys");
$q->add_outer_join("Employee.department.company.address");
$q->add_outer_join("Employee.department.employees.address");
$q->add_outer_join("Employee.department.company.contractors.businessAddress");

print "Querying for $q\n";

my $res = $q->results(as => 'jsonobjects', json => 'raw');

print $res;

open(my $out, '>', '/tmp/results.json');

print $out $res;

close $out;

