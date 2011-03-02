#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 16;
use Test::Exception;

use InterMine::Model;

my $model = InterMine::Model->new(file => 't/data/testmodel_model.xml');

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

is($comp_cd->get_field_by_name('secretarys')->referenced_classdescriptor->unqualified_name,
   'Secretary');

is($comp_cd->get_field_by_name('secretarys')->referenced_classdescriptor, 'Secretary');

is_deeply(
    [sort $comp_cd->attributes],
    [qw/name vatNumber/],
    "Gets attributes correctly",
);

is_deeply(
    [sort $comp_cd->references],
    [qw/CEO address/],
    "Gets references correctly",
);

is_deeply(
    [sort $comp_cd->collections],
    [qw /contractors departments oldContracts secretarys/],
    "Gets collections correctly",
);

ok($model->get_classdescriptor_by_name('Manager')->sub_class_of($emp_cd), 'Subclasses');

# from this class
ok($comp_cd->valid_field('departments'));

is($comp_cd->get_field_by_name('departments')->referenced_classdescriptor->unqualified_name,
   'Department');

is(scalar($comp_cd->fields()), 8);

is_deeply(
    [$model->get_classdescriptor_by_name("Manager")->get_ancestors],
    [qw/Manager Employee Employable Thing HasAddress ImportantPerson/],
    "Gets the correct inheritance list",
);
