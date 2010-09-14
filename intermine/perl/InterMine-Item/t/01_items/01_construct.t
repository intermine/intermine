#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 17;
use Test::Exception;
use Test::Warn;

use XML::Writer;
use InterMine::Model;

my $item = 'InterMine::Item';
my $doc = 'InterMine::Item::Document';

use_ok($item);
use_ok($doc);

my $model = new InterMine::Model(file => 't/data/testmodel_model.xml');
my $document = new_ok($doc => [model => $model], 'Can make a document');


throws_ok(sub {$document->add_item('beatnik')},
	  qr/beatnik not in the model/,
	  'Catches bad item creation');


my $emp1;
lives_ok(
    sub { $emp1 = $document->add_item(
        Employee => (
        name => "fred",
        age  => 40,
        ),
    )},
   "Makes an item ok",
) or BAIL_OUT("making items failed");

is($emp1->get("name"), "fred", "Sets a string field ok");
is($emp1->get('age'), 40, "Sets an int field ok");

my $emp2 = $document->add_item("Employee");
$emp2->set("name", "ginger");
$emp2->set("age", "50");

my $dept = $document->add_item("Department");
$dept->set("name", "big department");
$dept->set("employees", [$emp1, $emp2]);

ok(@{$dept->get("employees")} == 2);

my $company = $document->add_item("Company");
$company->set("name", "big company");
$dept->set("company", $company);

my @company_depts = @{$company->get("departments")};

my $emp3 = $document->add_item("CEO");
$emp3->set("name", "eric (ceo)");
$emp3->set("age", "12.5");

$dept->_add_to_collection("employees", $emp3);

ok(@{$dept->get("employees")} == 3);

my @employees = @{$dept->get("employees")};
is($employees[0]->get("name"), "fred");
is($employees[1]->get("name"), "ginger");
is($employees[2]->get("name"), "eric (ceo)");

my $sec1 = $document->add_item("Secretary");
$sec1->set("name", "secretary 1");

$emp3->_add_to_collection('secretarys', $sec1);

ok(@{$emp3->get('secretarys')} == 1);

my $sec2 = $document->add_item("Secretary");
$sec2->set("name", "secretary 2");

$emp3->_add_to_collection('secretarys', $sec2);

ok(@{$emp3->get('secretarys')} == 2);

throws_ok(sub {$emp3->set('no_such_field', 'some_value')},
	 qr/CEO.* field called: no_such_field/,
	  'Catches bad calls to set');

my $dept_exp = $document->add_item("Department");
$dept_exp->set("employees", [$emp1, $emp2]);

my $dept_got = $document->add_item("Department");
$dept_got->{id} = $dept_exp->{id}; # otherwise compare will fail

warning_like(sub {$dept_got->set("employees", [$emp1, undef, $emp2])}, 
	  qr/Undefined items passed as value/,
	  'Catches warnings for undefined items in a collection');

is_deeply($dept_got->get('employees'), $dept_exp->get('employees'), 'Ignores undefined items correctly');
