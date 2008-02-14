#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 11;

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


my $emp3 = $factory->make_item("CEO");
$emp3->set("name", "eric (ceo)");
$emp3->set("age", "12.5");

$dept->add_to_collection("employees", $emp3);

ok(@{$dept->get("employees")} == 3);

my @employees = @{$dept->get("employees")};
ok($employees[0]->get("name") eq "fred");
ok($employees[1]->get("name") eq "ginger");
ok($employees[2]->get("name") eq "eric (ceo)");

my $sec1 = $factory->make_item("Secretary");
$sec1->set("name", "secretary 1");

$emp3->add_to_collection('secretarys', $sec1);

ok(@{$emp3->get('secretarys')} == 1);

my $sec2 = $factory->make_item("Secretary");
$sec2->set("name", "secretary 2");

$emp3->add_to_collection('secretarys', $sec2);

ok(@{$emp3->get('secretarys')} == 2);

eval {
  $emp3->set('no_such_field', 'some_value');
  fail("shouldn't allow unknown fields to be set");
};

ok($@ =~ /CEO.* field called: no_such_field/)
