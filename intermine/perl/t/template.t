#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 19;
use Test::Exception;

BEGIN {
      use_ok('InterMine::Template');
}

my $module = 'InterMine::Template';
my $model = '../objectstore/model/testmodel/testmodel_model.xml';
my @methods = qw(new get_views get_constraints get_description
                 get_name);
can_ok($module, @methods);

dies_ok {my $t = $module->new()} "Building Template without args";
dies_ok {my $t = $module->new(file => 'fake_file')} "Building Template without model";
dies_ok {my $t = $module->new(file => 'fake_file', model => $model)} "Building Template with an invalid file";

my $bad_file     = 't/data/bad_xml.txt';
my $bad_string   = <<END
<template name="Probe_Gene">
  <query name="Probe_Gene">
    <unknown_tag>This should make it die</unknown_tag>
  </query>
</template>
END
    ;
dies_ok {my $t = $module->new(file => $bad_file, model => $model)} "Building Template with a bad file";
dies_ok {my $t = $module->new(string => $bad_string, model => $model)} "Building Template with a bad string";

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
dies_ok {my $t = $module->new(file => $multi_file, model => $model)} "Building Template with a multi template file";
dies_ok {my $t = $module->new(string => $multi_string, model => $model)} "Building Template with a multi template string";

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
my @f_args = (file => $good_file, model => $model);
my @s_args = (string => $good_string, model => $model);
my $t_from_file   = new_ok($module => \@f_args, 'Template');
can_ok($t_from_file, @methods);
my $t_from_string = new_ok($module => \@s_args, 'Template');
can_ok($t_from_string, @methods);

my $exp_name = 'Probe_Gene';
my $exp_desc = 'For specified Affymetrix probeset(s) show the corresponding gene.';
my @views    = qw(Gene.probeSets.primaryIdentifier Gene.primaryIdentifier Gene.symbol Gene.chromosomeLocation.object.primaryIdentifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end);
my $exp_con  = 'Gene.probeSets.primaryIdentifier = "1634044_at"';

my $t = $module->new(file => $good_file, model => $model);

is($t->get_name, $exp_name, "Template name");
is($t->get_description, $exp_desc, 'Template description');
my @got_views = $t->get_views;
is_deeply(\@got_views, \@views, 'Template views');
ok($t->get_constraints == 2, 'All constraints');
ok(grep({$_->is_editable} $t->get_constraints) == 1, 'Only editable constraints');
is(($t->get_constraints)[1]->as_string, $exp_con, 'Constraint as string');

