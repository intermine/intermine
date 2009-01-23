#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 12;
use Test::Exception;

use InterMine::Model;
use InterMine::PathQuery;

my $model = new InterMine::Model(file => '../objectstore/model/testmodel/testmodel_model.xml');

my $path_query = new InterMine::PathQuery($model);

dies_ok {$path_query->to_xml_string()} 'expected to_xml_string() to fail - no view';

$path_query->add_view('Department.name');

my @view = $path_query->view();
ok(@view == 1);
ok($view[0] eq 'Department.name');

$path_query->add_view('Department.name');

# add same path again:
@view = $path_query->view();
ok(@view == 1);
ok($view[0] eq 'Department.name');

dies_ok {$path_query->add_view('Illegal')} 'expected failure - illegal class';
dies_ok {$path_query->add_view('Department.wrong')} 'expected failure - illegal field';
dies_ok {$path_query->add_view('Department.company.wrong')} 'expected failure - illegal field';

$path_query->add_view('Department.company.name');

@view = $path_query->view();
ok(@view == 2, 'view length == 2');
ok($view[0] eq 'Department.name', 'add field');
ok($view[1] eq 'Department.company.name', 'add field');

my $expected_xml = q[<query name="" model="testmodel" view="Department.name Department.company.name" sortOrder="Department.name" constraintLogic=""></query>];

is ($expected_xml, $path_query->to_xml_string(), 'xml output');
