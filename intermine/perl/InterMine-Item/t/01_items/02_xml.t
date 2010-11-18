#!/usr/bin/perl

use strict;
use warnings;

use Test::XML tests => 1;

use InterMine::Model;
use InterMine::Item::Document;

my $output;

my $model = new InterMine::Model(file => 't/data/testmodel_model.xml');
my $document = new InterMine::Item::Document(model => $model, output => \$output);


my $emp1 = $document->add_item("Employee");
$emp1->set("name", "fred");
$emp1->set("age", "40");

my $emp2 = $document->add_item("Employee");
$emp2->set("name", "ginger");
$emp2->set("age", "50");

my $dept = $document->add_item("Department");
$dept->set("name", "big department");
$dept->set("employees", [$emp1, $emp2]);

my $company = $document->add_item("Company");
$company->set("name", "big company");
$dept->set("company", $company);

my @company_depts = @{$company->get("departments")};

my $xml_exp = q[
<items>
   <item id="0_1" class="Employee" implements="">
      <reference name="department" ref_id="0_3" />
      <attribute name="name" value="fred" />
      <attribute name="age" value="40" />
   </item>
   <item id="0_2" class="Employee" implements="">
      <reference name="department" ref_id="0_3" />
      <attribute name="name" value="ginger" />
      <attribute name="age" value="50" />
   </item>
   <item id="0_3" class="Department" implements="">
       <collection name="employees">
          <reference ref_id="0_1" />
          <reference ref_id="0_2" />
       </collection>
      <attribute name="name" value="big department" />
      <reference name="company" ref_id="0_4" />
   </item>
   <item id="0_4" class="Company" implements="">
      <attribute name="name" value="big company" />
   </item>
</items>
];

$document->write;
undef $document; #Testing document closure on destruction

is_xml($output, $xml_exp); 
