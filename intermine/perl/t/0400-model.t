#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 3;

my $module = 'InterMine::Model';

use_ok($module); # Test 1
my $model = new_ok($module => [file => '../objectstore/model/testmodel/testmodel_model.xml']); # Test 2
ok($model->model_name eq 'testmodel', 'Model has the right name'); # Test 3
