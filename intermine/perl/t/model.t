#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 30;

use InterMine::Model;

my $model = new InterMine::Model(file => '../objectstore/model/testmodel/testmodel_model.xml');

ok(scalar($model->get_all_classdescriptors()) == 17);

my $department_cd = $model->get_classdescriptor_by_name("Department");
my $name_field = $department_cd->get_field_by_name("name");
my $department_company_field = $department_cd->get_field_by_name("company");
my $employees_field = $department_cd->get_field_by_name("employees");
my $contractor_cd = $model->get_classdescriptor_by_name("Contractor");
my $contractor_companys_field = $contractor_cd->get_field_by_name("companys");
my $company_cd = $model->get_classdescriptor_by_name("Company");
my $secretarys_field = $company_cd->get_field_by_name("secretarys");
my $address_field = $company_cd->get_field_by_name("address");

ok($name_field->field_type() eq 'attribute');
ok($department_company_field->field_type() eq 'reference');
ok($employees_field->field_type() eq 'collection');
ok($contractor_companys_field->is_many_to_many());

my @fields = ($department_company_field, $employees_field,
   $contractor_companys_field, $secretarys_field, $address_field);

my @methods = (
               \&InterMine::Model::Reference::is_many_to_one,
               \&InterMine::Model::Reference::is_one_to_many,
               \&InterMine::Model::Reference::is_many_to_many,
               \&InterMine::Model::Reference::is_many_to_0,
               \&InterMine::Model::Reference::is_one_to_0,
              );
for (my $i = 0; $i<5; $i++) {
  my $field = $fields[$i];
  for (my $j = 0; $j<5; $j++) {
    my $method = $methods[$j];
    if ($i == $j) {
      ok(&$method($field));
    } else {
      ok(!&$method($field));
    }
  }
}
