#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 1;
use Test::Exception;

use InterMine::Model;
use InterMine::PathQuery;

my $model = new InterMine::Model(file => '../objectstore/model/testmodel/testmodel_model.xml');

my $path_query = new InterMine::PathQuery($model);

$path_query->add_view('Department.name');
$path_query->add_view('Department.company.name');

$path_query->add_constraint('Department.name != "Music department"');
$path_query->add_constraint('Department.name IS NOT NULL');
$path_query->add_constraint('Department.company.name = Woolworths');

my $expected_xml = q[<query name="" model="testmodel" view="Department.name Department.company.name" sortOrder="Department.name">
   <node path="Department.company.name" type="String">
      <constraint op="=" value="Woolworths" code="A"></constraint>
   </node>
   <node path="Department.name" type="String">
      <constraint op="!=" value="Music department" code="B"></constraint>
      <constraint op="IS NOT NULL" code="C"></constraint>
   </node>
</query>];

is ($expected_xml, $path_query->to_xml_string(), 'xml output');
