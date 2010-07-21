#!/usr/bin/perl

use strict;
use warnings;

use Test::XML tests => 12;
use Test::Exception;

use InterMine::Model;
use InterMine::PathQuery qw(AND OR);

#use Exception::Warning '%SIG' => 'die';

my $model = new InterMine::Model(file => '../objectstore/model/testmodel/testmodel_model.xml');

my $path_query = new InterMine::PathQuery($model);

$path_query->add_view('Department.name');
$path_query->add_view('Department.company.name');

my $expected_xml = q[<query name="" model="testmodel" view="Department.name Department.company.name"></query>];

is_xml ($path_query->to_xml_string(), $expected_xml, 'xml output');

$path_query->sort_order('Department.name');

$expected_xml = q[<query name="" model="testmodel" view="Department.name Department.company.name" sortOrder="Department.name"></query>];

is_xml ($path_query->to_xml_string(), $expected_xml, 'xml output with sort order');

my $depname_c = $path_query->add_constraint('Department.name != "Music department"');
my $not_null_c = $path_query->add_constraint('Department.name IS NOT NULL');
my $comp_name_c = $path_query->add_constraint('Department.company.name = Woolworths');

$expected_xml = q[<query name="" model="testmodel" view="Department.name Department.company.name" sortOrder="Department.name">
   <node path="Department.company.name" type="String">
      <constraint op="=" value="Woolworths" code="C"></constraint>
   </node>
   <node path="Department.name" type="String">
      <constraint op="!=" value="Music department" code="A"></constraint>
      <constraint op="IS NOT NULL" code="B"></constraint>
   </node>
</query>];

is_xml ($path_query->to_xml_string(), $expected_xml, 'xml output with constraints');

$comp_name_c->extra_value('NZ');

$expected_xml = q[<query name="" model="testmodel" view="Department.name Department.company.name" sortOrder="Department.name">
   <node path="Department.company.name" type="String">
      <constraint op="=" value="Woolworths" code="C" extraValue = "NZ"></constraint>
   </node>
   <node path="Department.name" type="String">
      <constraint op="!=" value="Music department" code="A"></constraint>
      <constraint op="IS NOT NULL" code="B"></constraint>
   </node>
</query>];

is_xml ($path_query->to_xml_string(), $expected_xml, 'xml with extraValue');


$path_query->logic(AND($depname_c, OR($not_null_c, $comp_name_c)));

$expected_xml = q[<query name="" model="testmodel" view="Department.name Department.company.name" sortOrder="Department.name" constraintLogic="A and (B or C)">
   <node path="Department.company.name" type="String">
      <constraint op="=" value="Woolworths" code="C" extraValue = "NZ"></constraint>
   </node>
   <node path="Department.name" type="String">
      <constraint op="!=" value="Music department" code="A"></constraint>
      <constraint op="IS NOT NULL" code="B"></constraint>
   </node>
</query>];

is_xml ($path_query->to_xml_string(), $expected_xml, 'xml output with oldstyle logic');

$path_query->logic($depname_c & ($not_null_c | $comp_name_c));

is_xml ($path_query->to_xml_string(), $expected_xml, 'xml output with newstyle logic - operators');

$path_query->logic("A and (B or C)");

is_xml ($path_query->to_xml_string(), $expected_xml, 'xml output with newstyle logic - logicstring');

throws_ok(sub {$path_query->logic("A and (B or C) and D")},
	       qr/No constraint with code D/,
	       "Bad logic string: Codes");
throws_ok(sub {$path_query->logic("A & (B | C)")},
	       qr/unexpected element in logic string/,
	       "Bad logic string: operators");
throws_ok(sub {$path_query->logic($depname_c && ($not_null_c || $comp_name_c))},
	       qr/unexpected element in logic string/,
	       "Bad logic: operators");
throws_ok(sub {$path_query->logic($depname_c & ($not_null_c | $comp_name_c) & $expected_xml)},
	       qr/Can't call method "isa" without a package or object reference/,
	       "Bad logic: operators");
throws_ok(sub {$path_query->logic('A' & ('B' | 'C'))},
	       qr/Invalid logic: not a ConstraintSet/,
	       "Bad logic - mixing styles");


