#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 20;
use Test::Exception;

### Setting up

my $bad_file     = 't/data/bad_xml.txt';
my $bad_string   = <<END
<template name="Probe_Gene">
  <query name="Probe_Gene">
    <unknown_tag>This should make it die</unknown_tag>
  </query>
</template>
END
    ;
my $bad_string2   = <<END
<template name="Probe_Gene">
  <query name="Unfinished query">
</template>
END
    ;



my $multi_file   = 't/data/multixml.txt';
my $multi_string = <<END
<template name="Probe_Gene" title="Affymetrix probeset --&gt; Gene." longDescription="For specified Affymetrix probeset(s) show the corresponding gene." comment="">
  <query name="Probe_Gene" model="genomic" view="Gene.probeSets.primaryIdentifier Gene.primaryIdentifier Gene.symbol Gene.chromosomeLocation.object.primaryIdentifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end" longDescription="For specified Affymetrix probeset(s) show the corresponding gene." sortOrder="Gene.probeSets.primaryIdentifier asc" constraintLogic="A and B">
    <pathDescription pathString="Gene.probeSets.chromosome" description="Probe &gt; chromosome">
    </pathDescription>
    <pathDescription pathString="Gene.probeSets.evidence" description="Dataset">
    </pathDescription>
    <pathDescription pathString="Gene.probeSets.chromosomeLocation" description="Probe &gt; chromosome location">
    </pathDescription>
    <pathDescription pathString="Gene.probeSets" description="Probe">
    </pathDescription>
    <node path="Gene" type="Gene">
    </node>
    <node path="Gene.organism" type="Organism">
    </node>
    <node path="Gene.organism.name" type="String">
      <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
      </constraint>
    </node>
    <node path="Gene.probeSets" type="ProbeSet">
    </node>
    <node path="Gene.probeSets.primaryIdentifier" type="String">
      <constraint op="=" value="1634044_at" description="" identifier="" editable="true" code="B">
      </constraint>
    </node>
  </query>
</template>
<template name="GeneOrganism1_OrthologueOrganism2" title="All genes organism1 --&gt; Orthologues organism2." longDescription="Show all the orthologues between two specified organisms. (Data Source: Inparanoid, Drosophila Consortium)." comment="">
  <query name="GeneOrganism1_OrthologueOrganism2" model="genomic" view="Homologue.gene.primaryIdentifier Homologue.gene.symbol Homologue.homologue.primaryIdentifier Homologue.homologue.symbol Homologue.inParanoidScore Homologue.dataSets.title" longDescription="Show all the orthologues between two specified organisms. (Data Source: Inparanoid, Drosophila Consortium)." sortOrder="Homologue.gene.primaryIdentifier asc" constraintLogic="A and B and C">
    <pathDescription pathString="Homologue.dataSets" description="Dataset">
    </pathDescription>
    <pathDescription pathString="Homologue.gene" description="Organism1 &gt; gene">
    </pathDescription>
    <pathDescription pathString="Homologue.homologue" description="Organism2 &gt; gene">
    </pathDescription>
    <node path="Homologue" type="Homologue">
    </node>
    <node path="Homologue.gene" type="Gene">
    </node>
    <node path="Homologue.gene.organism" type="Organism">
    </node>
    <node path="Homologue.gene.organism.name" type="String">
      <constraint op="=" value="Drosophila melanogaster" description="Show the predicted orthologues between:" identifier="" editable="true" code="A">
      </constraint>
    </node>
    <node path="Homologue.homologue" type="Gene">
    </node>
    <node path="Homologue.homologue.organism" type="Organism">
    </node>
    <node path="Homologue.homologue.organism.name" type="String">
      <constraint op="=" value="Caenorhabditis elegans" description="and:" identifier="" editable="true" code="B">
      </constraint>
    </node>
    <node path="Homologue.type" type="String">
      <constraint op="=" value="orthologue" description="" identifier="" code="C">
      </constraint>
    </node>
  </query>
</template>
END
    ;

my $good_file   =  't/data/good_xml.txt';
my $good_string =  <<END
<template name="Probe_Gene" title="Affymetrix probeset --&gt; Gene." longDescription="For specified Affymetrix probeset(s) show the corresponding gene." comment="">
  <query name="Probe_Gene" model="genomic" view="Gene.probeSets.primaryIdentifier Gene.primaryIdentifier Gene.symbol Gene.chromosomeLocation.object.primaryIdentifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end" longDescription="For specified Affymetrix probeset(s) show the corresponding gene." sortOrder="Gene.probeSets.primaryIdentifier asc" constraintLogic="A and B">
    <pathDescription pathString="Gene.probeSets.chromosome" description="Probe &gt; chromosome">
    </pathDescription>
    <pathDescription pathString="Gene.probeSets.evidence" description="Dataset">
    </pathDescription>
    <pathDescription pathString="Gene.probeSets.chromosomeLocation" description="Probe &gt; chromosome location">
    </pathDescription>
    <pathDescription pathString="Gene.probeSets" description="Probe">
    </pathDescription>
    <node path="Gene" type="Gene">
    </node>
    <node path="Gene.organism" type="Organism">
    </node>
    <node path="Gene.organism.name" type="String">
      <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
      </constraint>
    </node>
    <node path="Gene.probeSets" type="ProbeSet">
    </node>
    <node path="Gene.probeSets.primaryIdentifier" type="String">
      <constraint op="=" value="1634044_at" description="" identifier="" editable="true" code="B">
      </constraint>
    </node>
  </query>
</template>
END
    ;

my $module = 'InterMine::Template';
my $model = '../objectstore/model/testmodel/testmodel_model.xml';
my @methods = qw(new 
                 get_views 
                 get_constraints 
                 get_editable_constraints
                 get_description
                 get_name
                 get_sort_order
                 get_logic
                 as_path_query
                 to_xml_string);

### Tests

use_ok($module); # Test 1
can_ok($module, @methods); # Test 2

throws_ok(sub {my $t = $module->new()}, qr/needs a file or string argument/, # Test 3
    "Building Template without args");
throws_ok(sub {my $t = $module->new(file => $good_file)}, # Test 4
	       qr/We need a model to build templates with/,
	       "Building Template without model");
throws_ok(sub {my $t = $module->new(file => 'fake_file', model => $model)}, # Test 5
	  qr/A valid file must be specified: we got/,
	  "Building Template with a fake file");

throws_ok(sub {my $t = $module->new(file => $bad_file, model => $model)}, #Test 6
	  qr/unexpected element/,
	  "Building Template with a bad file");

throws_ok(sub {my $t = $module->new(string => $bad_string, model => $model)}, #Test 7
	  qr/unexpected element/,
	  "Building Template with a bad string - unknown element");

throws_ok(sub {my $t = $module->new(string => $bad_string2, model => $model)}, #Test 8
	  qr/mismatched tag/,
	  "Building Template with a bad string - bad xml");

throws_ok(sub {my $t = $module->new(file => $multi_file, model => $model)}, # Test 9
	  qr/junk after document element/,
	  "Building Template with a multi template file");

throws_ok(sub {my $t = $module->new(string => $multi_string, model => $model)}, # Test 10
	  qr/junk after document element/,
	  "Building Template with a multi template string");


my @f_args = (file => $good_file, model => $model);
my @s_args = (string => $good_string, model => $model);
my $t_from_file   = new_ok($module => \@f_args, 'Template'); # Test 11
can_ok($t_from_file, @methods); # Test 12
my $t_from_string = new_ok($module => \@s_args, 'Template'); # Test 13
can_ok($t_from_string, @methods); # Test 14

my $exp_name = 'Probe_Gene';
my $exp_desc = 'For specified Affymetrix probeset(s) show the corresponding gene.';
my @views    = qw(Gene.probeSets.primaryIdentifier Gene.primaryIdentifier Gene.symbol Gene.chromosomeLocation.object.primaryIdentifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end);
my $exp_con  = 'Gene.probeSets.primaryIdentifier = "1634044_at"';

my $t = $t_from_file;

is($t->get_name, $exp_name, "Template name"); # Test 15
is($t->get_description, $exp_desc, 'Template description'); # Test 16
is_deeply([$t->get_views], \@views, 'Template views'); # Test 17
ok($t->get_constraints == 2, 'Fetches all constraints'); # Test 18
ok($t->get_editable_constraints == 1, 'Returns only editable constraints'); # Test 19
is(($t->get_editable_constraints)[0]->as_string, $exp_con, # Test 20
   'Parses the constraints correctly'); 
