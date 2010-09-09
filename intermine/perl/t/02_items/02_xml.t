#!/usr/bin/perl

use strict;
use warnings;

use Test::XML tests => 1;

use XML::Writer;
use InterMine::Model;
use InterMine::Item;
use InterMine::ItemFactory;

use IO::String;


my $model = new InterMine::Model(file => 't/data/testmodel_model.xml');
my $factory = new InterMine::ItemFactory(model => $model);

my $emp1 = $factory->make_item("Employee");
$emp1->set("name", "fred");
$emp1->set("age", "40");

my $emp2 = $factory->make_item("Employee");
$emp2->set("name", "ginger");
$emp2->set("age", "50");

my $dept = $factory->make_item("Department");
$dept->set("name", "big department");
$dept->set("employees", [$emp1, $emp2]);

my $company = $factory->make_item("Company");
$company->set("name", "big company");
$dept->set("company", $company);

my @company_depts = @{$company->get("departments")};

my $output = new IO::String();
my $writer = new XML::Writer(DATA_MODE => 1, DATA_INDENT => 3, OUTPUT => $output);
$writer->startTag('items');
for my $item ($emp1, $emp2, $dept, $company) {
  $item->as_xml($writer);
}
$writer->endTag('items');

my $xml_exp = q[
<items>
   <item id="0_1" class="" implements="Employee">
      <reference name="department" ref_id="0_3" />
      <attribute name="name" value="fred" />
      <attribute name="age" value="40" />
   </item>
   <item id="0_2" class="" implements="Employee">
      <reference name="department" ref_id="0_3" />
      <attribute name="name" value="ginger" />
      <attribute name="age" value="50" />
   </item>
   <item id="0_3" class="" implements="Department">
       <collection name="employees">
          <reference ref_id="0_1" />
          <reference ref_id="0_2" />
       </collection>
      <attribute name="name" value="big department" />
      <reference name="company" ref_id="0_4" />
   </item>
   <item id="0_4" class="" implements="Company">
      <attribute name="name" value="big company" />
   </item>
</items>
];

is_xml(${$output->string_ref}, $xml_exp); 
