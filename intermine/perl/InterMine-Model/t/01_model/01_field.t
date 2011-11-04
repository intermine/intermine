#!/usr/bin/perl

use strict;
use warnings;
use Carp 'confess';

$SIG{__DIE__} = \&Carp::confess;

use Test::More tests => 31;

use InterMine::Model;

my $model = new InterMine::Model(file => 't/data/testmodel_model.xml');

my $no_of_classes = scalar($model->get_all_classdescriptors);

ok(($no_of_classes) == 19); # Test 1

my $department_cd = $model->get_classdescriptor_by_name("Department");
my $name_field = $department_cd->get_field_by_name("name");
my $department_company_field = $department_cd->get_field_by_name("company");
my $employees_field = $department_cd->get_field_by_name("employees");
my $contractor_cd = $model->get_classdescriptor_by_name("Contractor");
my $contractor_companys_field = $contractor_cd->get_field_by_name("companys");
my $company_cd = $model->get_classdescriptor_by_name("Company");
my $secretarys_field = $company_cd->get_field_by_name("secretarys");
my $address_field = $company_cd->get_field_by_name("address");

ok($name_field->isa('InterMine::Model::Attribute'));               # Test 2
ok($department_company_field->isa('InterMine::Model::Reference')); # Test 3
ok($employees_field->isa('InterMine::Model::Collection'));         # Test 4

is($department_company_field->field_class->unqualified_name, "Department", 
    "Field has a reference to the class");
is($department_company_field->class_name, "Department", 
    "Field has a reference to the class");

my @fields = ($department_company_field, $employees_field,
   $contractor_companys_field, $secretarys_field, $address_field);

my @relationship = ("many to one", "one to many", "many to many", "one of many", "unique");

my @methods = (
               \&InterMine::Model::Reference::is_many_to_one,
               \&InterMine::Model::Reference::is_one_to_many,
               \&InterMine::Model::Reference::is_many_to_many,
               \&InterMine::Model::Reference::is_many_to_0,
               \&InterMine::Model::Reference::is_one_to_0,
              );

# Tests 5-29 (5*5 tests = 25)
for (my $i = 0; $i<5; $i++) {
  my $field = $fields[$i];
  my $relationship = $relationship[$i];
  for (my $j = 0; $j<5; $j++) {
    my $method = $methods[$j];
    if ($i == $j) {
      ok(&$method($field), "field is $relationship");
    } else {
      ok(!&$method($field), "field isn't $relationship");
    }
  }
}
