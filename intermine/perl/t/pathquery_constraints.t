#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 2;
use Test::Exception;

use InterMine::Model;
use InterMine::PathQuery qw(AND OR);

#use Exception::Warning '%SIG' => 'die';

my $model = new InterMine::Model(file => '../objectstore/model/testmodel/testmodel_model.xml');

my $path_query = new InterMine::PathQuery($model);

$path_query->add_view('Department.name');
$path_query->add_view('Department.company.name');

my $depname_c = $path_query->add_constraint('Department.name != "Music department"');
my $not_null_c = $path_query->add_constraint('Department.name IS NOT NULL');
my $comp_name_c = $path_query->add_constraint('Department.company.name = Woolworths');

my $expected_xml = q[<query name="" model="testmodel" view="Department.name Department.company.name" sortOrder="Department.name">
   <node path="Department.company.name" type="String">
      <constraint op="=" value="Woolworths" code="C"></constraint>
   </node>
   <node path="Department.name" type="String">
      <constraint op="!=" value="Music department" code="A"></constraint>
      <constraint op="IS NOT NULL" code="B"></constraint>
   </node>
</query>];

is ($path_query->to_xml_string(), $expected_xml, 'xml output');

$path_query->logic(AND($depname_c, OR($not_null_c, $comp_name_c)));

$expected_xml = q[<query name="" model="testmodel" view="Department.name Department.company.name" sortOrder="Department.name" constraintLogic="A and (B or C)">
   <node path="Department.company.name" type="String">
      <constraint op="=" value="Woolworths" code="C"></constraint>
   </node>
   <node path="Department.name" type="String">
      <constraint op="!=" value="Music department" code="A"></constraint>
      <constraint op="IS NOT NULL" code="B"></constraint>
   </node>
</query>];

is ($path_query->to_xml_string(), $expected_xml, 'xml output');
