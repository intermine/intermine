#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 21;
use Test::Exception;
use Test::MockObject::Extends;

use InterMine::Model;

### Setting up

my $module = 'InterMine::WebService::Service::TemplateService';
my $url = 'http://www.fakeurl.org/query/service';
my @methods = qw/new 
                 get_templates
                 get_template
                 search_for 
                 get_result
                 /;

my $fake_content = 
q{<template-queries><template name="Probe_Gene" title="Affymetrix probeset --&gt; Gene." longDescription="For specified Affymetrix probeset(s) show the corresponding gene." comment="">
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
</template></template-queries>};

my $fake_response = Test::MockObject::Extends->new;
$fake_response->set_true('is_error')
    ->mock(content => sub {return $fake_content})
    ->mock(status_line => sub {return 'mock http error'})
;

my @empty_array = ();

my $exp_req_params = {
          'constraint1' => 'Gene.probeSets.primaryIdentifier',
          'value1' => '1634044_at',
          'format' => 'tab',
          'name' => 'fake_search',
          'op1' => '='
        };

### Tests

use_ok($module); # Test 1
can_ok($module, @methods); # Test 2

my $service = new_ok($module => [$url], 'Service'); # Test 3
isa_ok($service, 'InterMine::WebService::Core::Service', 'Inherits ok'); # Test 4

# ### Test _make_templates_from_xml
my $model = '../objectstore/model/testmodel/testmodel_model.xml';
my $ms = $service->{model_service};
$ms = Test::MockObject::Extends->new($ms);
$ms->mock(get_model => sub {return InterMine::Model->new(file => $model)});

### Test get_templates

# isolate execute_request, which is tested in webservice_core_service.t
$service = Test::MockObject::Extends->new($service);
$service->mock(execute_request => sub {return $fake_response});

# when we already have templates, we return them, instead of fetching a new list

my $test_array = [qw/one two three/];
$service->{'templates'} = $test_array;

is_deeply([$service->get_templates], $test_array, # Test 5
	  'Retrieves cached lists of templates'); 
delete $service->{'templates'};

# Catch communication errors

throws_ok(sub {$service->get_templates}, # Test 6
	 qr/Fetching templates failed.*mock http error/,
	 'Catches http error ok');

# Check for successful queries

$fake_response->set_false('is_error'); 
my ($template) = $service->get_templates;
isa_ok($template, 'InterMine::Template', 'Returned template'); # Test 7

is_deeply($template, $service->{templates}->[0], 'Stores templates ok'); # Test 8


### Test search_for

throws_ok( sub {$service->search_for}, qr/You need a keyword/, # Test 9
	   'Catches search for without a keyword');

my @kwijibobs = $service->search_for('kwijibob');
is_deeply(\@kwijibobs, \@empty_array, 'Return empty array for failed search'); # Test 10

my ($found) = $service->search_for('probe');
is_deeply($found, $template, "Returns one result from search ok"); # Test 11

my @founds = $service->search_for('gene');
my @all    = @{$service->{templates}};
is_deeply(\@founds, \@all, 'Can return multiple results from search ok'); # Test 12

### Test get_template

throws_ok( sub {$service->get_template}, qr/You need a name/, # Test 13
	   'Catches get_template without a name');
is($service->get_template('kwijibob'), undef, 'Return undef for unfound template'); # Test 14
is_deeply($service->get_template('Probe_Gene'), $template, # Test 15
	  'Can get a template by name');

$service->mock(get_templates => sub { # mock template to return ambiguous results
    my @t = @{shift->{templates}};
	$_->{pq}->{name} = 'fake_search' for (@t);
    return @t;
   }
);

is($service->get_template('fake_search'), undef, 'Return undef for multiple results'); # Test 16
$service->unmock('get_templates');

### Test get_result

throws_ok( sub {$service->get_result}, # Test 17
	   qr/get_result needs a valid InterMine::Template/,
	   'Catches lack of template');

$service->mock(execute_request => sub {my ($self, $req) = @_; return $req});

lives_ok(sub {$service->get_result($template)}, 'Happily processes a valid template'); # Test 18
my $ret = $service->get_result($template);
isa_ok($ret, 'InterMine::WebService::Core::Request', 'Results request'); # Test 19
is($ret->get_url, $url.'/template/results', 'Constructs url ok'); #Test 20
is_deeply({$ret->get_parameters}, $exp_req_params, 'Sets parameters ok'); # Test 21
