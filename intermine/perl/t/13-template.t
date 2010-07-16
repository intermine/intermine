#!/usr/bin/perl

use strict;
use warnings;

use List::MoreUtils qw/mesh/;
use InterMine::Model;

use XML::Rules;


my @rules = (
    _default => 'no content',
     # as hashes so we don't have to worry about array order
    node => 'no content by path',      
    constraint => 'no content by code',
    query => sub { 
	my ($name, $attr) = @_;
	 # It will be in the template, if at all
	delete $attr->{longDescription};
	delete $attr->{_content};
	$name => $attr;
    },
    template => sub {
	my $attr = $_[1];
	$attr->{title} = delete $attr->{description}
  	    unless (defined $attr->{title});
	for (keys %$attr) {
	    delete $attr->{$_} unless $attr->{$_} =~ /\S/;
	}
	return %$attr;
    },
    );

my $parser = XML::Rules->new(rules => \@rules);

### Setting up

############################################################
############ Get the xml for the bad templates
my $bad_templates  = 't/data/bad_templates.xml';

my $bad_content;
open my $BFH, '<', $bad_templates or die "Cannot open $bad_templates, $!";
for (<$BFH>) {
    $bad_content .= $_ unless /template-queries/;
}
close $BFH or die "Cannot close $bad_templates, $!";

#$bad_content =~ s/^.*<template-queries>(.*)<\/template-queries>.*$/$1/s;
my @baddies  = split "\n\n", $bad_content;
my @baddies_names = (qw/unknown_tag 
                        bad_class 
                        bad_attr 
                        malformed_xml 
                        unfinished 
                        empty_view 
                        no_constraints 
                        sort_order_not_in_query 
                        descr_and_title 
                        no_name 
                        names_differ
                        bad_logic/);
my %baddies = mesh(@baddies_names, @baddies);
my @baddies_errors = (
                       'unexpected element: unknown_tag',
                       'illegal path',
                       'illegal path',
                       'not well-formed',
                       'mismatched tag',
                       'No view set',
                       'Invalid template: no editable constraints',
                       'is not in the query',
                       'both description and title',
                       'No name attribute',
                       'Names for query and template differ',
                       'No constraint with code',
    );
my %exp_err_for = mesh(@baddies_names, @baddies_errors);
############################################################

############################################################
############ Get the xml for the good templates
my $good_templates = '../api/test/resources/default-template-queries.xml';

my $good_content;
open my $GFH, '<', $good_templates or die "Cannot open $good_templates, $!";
$good_content .= $_ for <$GFH>;
close $GFH or die "Cannot close $good_templates, $!";

$good_content =~ s/^.*<template-queries>(.*)<\/template-queries>.*$/$1/s;

my @goodies = split /template>\s*<template/, $good_content;
map {s[(</)\s*$][$1template>]s; s[^(\s+name)][<template$1]s} @goodies;
############################################################

my $multi_file = 't/data/multixml.txt';

my $module = 'InterMine::Template';
my $modelf = '../objectstore/model/testmodel/testmodel_model.xml';
my $model  = InterMine::Model->new(file => $modelf);
my @methods = qw(new 
                 get_editable_constraints
                 get_description
                 get_title
                 get_comment
                 get_source_string
                 to_xml_string);

my @inherited_methods = qw/add_view
                           view
                           get_views
                           sort_order
                           get_sort_order
                           add_constraint
                           get_all_constraints
                           AND
                           OR
                           get_logic
                           logic
                           to_query_xml
                           get_node_paths
                           get_constraints_on
                           get_described_paths
                           get_description_for
                           get_name
                           get_description
                           type_hash
                           type_of
                           model/;

use Test::More;
plan tests => (12 + @goodies + @baddies);
use Test::Exception;


### Tests

use_ok($module); # Test 1
can_ok($module, @methods); # Test 2
can_ok($module, @inherited_methods); # Test 3

throws_ok(sub {my $t = $module->new()}, qr/needs a file or string argument/, # Test 4
    "Building Template without args");
throws_ok(sub {my $t = $module->new(string => $goodies[0])}, # Test 5
	       qr/We need a model to build templates with/,
	       "Building Template without model");
throws_ok(sub {my $t = $module->new(string => $goodies[0], model => 'Not a model')}, # Test 6
	       qr/Invalid model/,
	       "Building Template with a bad model");
throws_ok(sub {my $t = $module->new(file => 'fake_file', model => $model)}, # Test 7
	  qr/A valid file must be specified: we got/,
	  "Building Template with a fake file");

throws_ok(sub {my $t = $module->new(file => $multi_file, model => $model)}, # Test 8
	  qr/junk after document element/,
	  "Building Template with a multi template file");


my @args = (string => $goodies[0], model => $model);
my $t    = new_ok($module => \@args, 'Makes new Template which'); # Test 9
isa_ok($t, 'InterMine::PathQuery', 'Inherits OK'); #Test 10
isa_ok($t, 'InterMine::ModelOwner', 'Inherits OK');#Test 11
can_ok($t, @methods, @inherited_methods); # Test 12

############################################################
# Test ability to parse good templates
############################################################

my $c;

for my $xmlstring (@goodies) {
    my $t = $module->new(string => $xmlstring, model => $model);
    is_deeply($parser->parse($t->to_xml_string),
	      $parser->parse($xmlstring),
	      "Successfully parses good template: ".++$c.' of '.@goodies);
}

############################################################
# Test ability to throw errors at bad templates
############################################################

while (my ($name, $xmlstring) = each(%baddies)) {
    throws_ok( sub {my $t = $module->new(string => $xmlstring, model => $model)},
	       qr/$exp_err_for{$name}/,
	       "Raises error parsing bad template: $name");
}
