#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 2;

use InterMine::Model;

my $model = new InterMine::Model(file => '../objectstore/model/testmodel/testmodel_model.xml');

my $emp_cd = $model->get_classdescriptor_by_name("Employee");

# from parent class
ok($emp_cd->valid_field('name'));

# from this class
ok($emp_cd->valid_field('age'));
