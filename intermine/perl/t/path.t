#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 9;
use Test::Exception;

use InterMine::Model;
use InterMine::Path;

my $model = new InterMine::Model(file => '../objectstore/model/testmodel/testmodel_model.xml');

my $path_string = 'Department.company.name';

InterMine::Path->validate($model, $path_string);

my $path = new InterMine::Path($model, $path_string);

my @parts = $path->parts();

is(scalar(@parts), 3, $path_string . ' has three parts');

is($parts[0]->name(), 'org.intermine.model.testmodel.Department',
   'class name of start of path');
is(ref $parts[0], 'InterMine::Model::ClassDescriptor',
   'start of path is a class descriptor');
is($parts[1]->field_name(), 'company', 'reference name of middle of path');
is(ref $parts[1], 'InterMine::Model::Reference',
   'middle of path is a reference descriptor');
is($parts[2]->field_name(), 'name', 'attribute name of end of path');
is(ref $parts[2], 'InterMine::Model::Attribute',
   'end of path is a reference descriptor');

dies_ok {InterMine::Path->validate($model, '')} 'invalid path';
dies_ok {InterMine::Path->validate($model, 'Foo')} 'invalid path';
dies_ok {InterMine::Path->validate($model, 'Department.foo')} 'invalid path';
dies_ok {InterMine::Path->validate($model, 'Department.company.foo')} 'invalid path';
