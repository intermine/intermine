#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 4;

use XML::Writer;
use InterMine::Model;
use InterMine::Item;
use InterMine::ItemFactory;

my $model = new InterMine::Model(file => '../objectstore/model/testmodel/testmodel_model.xml');
my $factory = new InterMine::ItemFactory(model => $model);

my $emp1 = $factory->make_item("Employee");
$emp1->set("name", "fred");
$emp1->set("age", "40");

ok($emp1->get("name") eq "fred");

my $emp2 = $factory->make_item("Employee");
$emp2->set("name", "ginger");
$emp2->set("age", "50");

my $dept = $factory->make_item("Department");
$dept->set("name", "big department");
$dept->set("employees", [$emp1, $emp2]);

ok(@{$dept->get("employees")} == 2);

my $company = $factory->make_item("Company");
$company->set("name", "big company");
$dept->set("company", $company);

my @company_depts = @{$company->get("departments")};

ok(scalar(@company_depts) == 1);

ok($company_depts[0]->get("name") eq "big department");
