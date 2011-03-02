#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 41;
use Test::Exception;
use InterMine::Model::TestModel;
use Webservice::InterMine::Path qw(:validate);

# internal functions we would like access to for testing
my $parse      = Webservice::InterMine::Path->can('_parse');
my $class_of   = Webservice::InterMine::Path->can('class_of');
my $next_class = Webservice::InterMine::Path->can('next_class');
my $last_bit   = Webservice::InterMine::Path->can('last_bit');

my $model = InterMine::Model::TestModel->instance;

my $good_path_string = 'Department.employees.name';
my $bad_class_string = 'Foo.bar.baz';
my $bad_ref_string   = 'Department.foo.baz';
my $bad_att_string   = 'Department.employees.wibble';
my $short_string     = 'Department';
my $two_bit_string   = 'Department.employees';

my $class = $model->get_classdescriptor_by_name('Department'); #Company -> Company
my $ref   = $class->get_field_by_name('employees');              #address -> Address
my $att   = $ref->referenced_classdescriptor->get_field_by_name('name');

is($class_of->($class), 'Department', 'Gets class of class');
is($class_of->($ref),   'Employee',   'Gets class of reference');
is($class_of->($att),   undef,       'Returns undef for attributes');


is($next_class->($class, $model), 'Department', 'Next class of a cd is the cd');
is($next_class->($ref, $model), 'Employee', 'Next class of a ref is the ref cd');
is($next_class->($att, $model), undef, 'Next class of an att is undef');
is($next_class->($ref, $model, 'Manager'), 'Manager', 
   'Next class of a subclassed path is the subclass');

throws_ok( sub {$next_class->($ref, $model, 'Foo')},
	   qr/Foo not in the model/,
	   'Catches bad next class');
dies_ok( sub {$next_class->($model, $ref)}, 'Dies on bad input');
my @parts;
lives_ok(sub {@parts = $parse->($model, $good_path_string)}, 
    'Can parse a good path');
is(scalar(@parts), 3, 'Gets the number of parts right');
like($parts[0]->name(), qr/Department$/, 'class name of start of path');
ok($parts[0]->isa('InterMine::Model::ClassDescriptor'), 
    'start of path is a class descriptor');
is($parts[1]->name(), 'employees', 'reference name of middle of path');
ok($parts[1]->isa('InterMine::Model::Reference'),
    'middle of path is a reference descriptor');
is($parts[2]->name(), 'name', 'attribute name of end of path');
ok($parts[2]->isa('InterMine::Model::Attribute'),
   'end of path is a reference descriptor');

throws_ok(sub {$parse->($model, $bad_class_string)},
        qr/Foo not in the model/,
        'Catches bad top class');
throws_ok(sub {$parse->($model, $bad_ref_string)},
        qr/illegal path \($bad_ref_string\): can't find field "foo"/,
        'Catches bad reference');
throws_ok(sub {$parse->($model, $bad_att_string)},
        qr/illegal path \($bad_att_string\): can't find field "wibble"/,
        'Catches bad attribute');

dies_ok(sub {validate_path($good_path_string)},
	 'Dies calling validate_path without model');
dies_ok(sub {validate_path($model, $good_path_string, [])},
	 'Dies calling validate_path with an arrayref');
dies_ok(sub {validate_path($model, {})},
	 'Dies calling validate_path with a typehash and no path');
is(validate_path($model, $good_path_string), undef, 'Returns undef to validate a good path');

like(validate_path($model, $bad_class_string), 
     qr/Foo not in the model/, 
     'Returns errors for top class');
like(validate_path($model, $bad_ref_string), 
     qr/illegal path \($bad_ref_string\): can't find field "foo"/,
     'Returns errors for bad reference');
like(validate_path($model, $bad_att_string), 
     qr/illegal path \($bad_att_string\): can't find field "wibble"/,
     'Returns errors for bad attribute');

is($last_bit->($model, $short_string), $class, 
   'Can get the last bit off a one bit name');
is($last_bit->($model, $two_bit_string), $ref, 
   'Can get the last bit off a two bit name');
is($last_bit->($model, $good_path_string), $att, 
   'Can get the last bit off a longer name');
dies_ok( sub {$last_bit->($good_path_string, $model)}, 
	 'last_bit dies on bad arguments');

is(end_is_class($model, $short_string), undef, 
   'end_is_class returns no errors for a class');
is(end_is_class($model, $two_bit_string), undef, 
   'end_is_class returns no errors for a class and ref');
like(end_is_class($model, $good_path_string),
     qr/$good_path_string: name is a .*String, not a class/,
     'end_is_class returns errors for a class and ref and attribute');
dies_ok( sub {$last_bit->($good_path_string, $model)}, 
	 'end_is_class dies on bad arguments');

is(b_is_subclass_of_a($model, 'Department.employees', 'Manager'), undef, 
   'No errors if b is a subclass of a');
like(b_is_subclass_of_a($model, 'Manager', 'Employee'),
      qr/Employee.*not a subclass of Manager/,
      'Errors if b is not a subclass of a');
is(b_is_subclass_of_a($model, 'Foo', 'Bar'), undef, 
   'Bad paths do not throw an error (they get validated by validate_path)');

is(a_is_subclass_of_b($model, 'Department.manager', 'Employee'), undef, 
   'No errors if b is a subclass of a');
like(a_is_subclass_of_b($model, 'Employee', 'Manager'),
      qr/Employee.*not a subclass of Manager/,
      'Errors if b is not a subclass of a');
is(a_is_subclass_of_b($model, 'Foo', 'Bar'), undef, 
   'Bad paths do not throw an error (they get validated by validate_path)');

