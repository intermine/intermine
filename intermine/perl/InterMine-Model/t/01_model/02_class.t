#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 11;
use Test::Exception;

use InterMine::Model;

my $model = new InterMine::Model(file => 't/data/testmodel_model.xml');

ok(my $emp_cd = $model->get_classdescriptor_by_name("Employee"), "Can get class descr by name");
throws_ok(sub {$model->get_classdescriptor_by_name}, 
	  qr/no classname passed to get_classdescriptor_by_name/,
	  'Catches lack of classname');
# from parent class
ok($emp_cd->valid_field('name'));

# from this class
ok($emp_cd->valid_field('age'));

ok(!$emp_cd->valid_field('not_valid'));

my $comp_cd = $model->get_classdescriptor_by_name("Company");

# from parent class
ok($comp_cd->valid_field('secretarys'));

is($comp_cd->get_field_by_name('secretarys')->referenced_classdescriptor()->name(),
   'Secretary');

ok($model->get_classdescriptor_by_name('Manager')->sub_class_of($emp_cd), 'Subclasses');

# from this class
ok($comp_cd->valid_field('departments'));

is($comp_cd->get_field_by_name('departments')->referenced_classdescriptor()->name(),
   'Department');

is(scalar($comp_cd->fields()), 8);
