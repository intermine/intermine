#!/usr/bin/perl

use strict;
use warnings;

use Test::More;
use Test::Exception;
use Test::MockObject;
use List::MoreUtils qw/mesh/;
use InterMine::Model::TestModel;
use XML::Rules;

my $module = 'Webservice::InterMine::Query::Template';

eval "require $module";

my @rules = (
    _default => 'as is',

    # as hashes so we don't have to worry about array order
    constraint => sub {
        my $key = delete( $_[1]->{code} );
        if ( not exists $_[1]->{switchable} ) {
            $_[1]->{switchable} = 'locked';
        }
        $key => $_[1];
    },
    query => sub {
        my ( $name, $attr ) = @_;
        delete $attr->{sortOrder};    # The template might not have this
        delete $attr->{longDescription};
        $name => $attr;
    },
    template => sub {
        my $attr = $_[1];
        $attr->{title} = delete $attr->{description}
          unless ( defined $attr->{title} );
        for ( keys %$attr ) {
            delete $attr->{$_} unless $attr->{$_} =~ /\S/;
        }
        return %$attr;
    },
);

my $parser = XML::Rules->new(
    rules           => \@rules,
    normalisespaces => 1,
    stripspaces     => 3|4,
);

############################################################
############ Get the xml for the good templates
my $good_templates = 't/data/default-template-queries.xml';

my $good_content;
open my $GFH, '<', $good_templates
  or die "Cannot open $good_templates, $!";
$good_content .= $_ for <$GFH>;
close $GFH or die "Cannot close $good_templates, $!";

$good_content =~ s/^.*<template-queries>(.*)<\/template-queries>.*$/$1/s;
my @goodies = $good_content =~ m!(<template .*?</template>)!sg;
my @sort_order = (
    'Employee.name', 'Employee.name',
    'Employee.name', 'Employee.name',
    'Employee.name', 'Employee.name',
    'Contractor.id', 'Manager.name',
    'Company.name',  'Company.name',
    'Company.name',  'Employee.name', 'Employee.name',
    'Company.departments.employees.name',
    'Employee.name',
);

############################################################

############################################################
############ Get the xml for the bad templates
my $bad_templates = 't/data/bad_templates.xml';

my $bad_content;
open my $BFH, '<', $bad_templates
  or die "Cannot open $bad_templates, $!";
for (<$BFH>) {
    $bad_content .= $_ unless /template-queries/;
}
close $BFH or die "Cannot close $bad_templates, $!";

my @baddies = split "\n\n", $bad_content;

my @baddies_names = (
    qw/unknown_tag
      bad_class
      bad_attr
      malformed_xml
      unfinished
      empty_view
      no_constraints
      descr_and_title
      no_name
      names_differ
      bad_logic
      Inconsistent
      BadFlags
      /
);
my %baddies = mesh( @baddies_names, @baddies );
my @baddies_errors = (
    'unexpected element: unknown_tag',
    'can\'t find field "BadClass"',
    'illegal path',
    'not well-formed',
    'mismatched tag',
    'No view in query',
    'Invalid template: no editable constraints',
    'both description and title',
    'No name attribute on template node',
    'We have two names and they differ',
    'No constraint with code',
    'can\'t find field "Employee"',
    'Only editable constraints can be switchable',
);
my %exp_err_for = mesh( @baddies_names, @baddies_errors );

my $model = InterMine::Model::TestModel->instance;

my $service = Test::MockObject->new;
$service->set_isa('Webservice::InterMine::Service');

############################################################
# Test ability to parse good templates
############################################################

plan tests => ( ( @goodies * 2 ) + @baddies );

my $c;

for my $xmlstring (@goodies) {
    my $t = $module->new(
        model         => $model,
        service       => $service,
        source_string => $xmlstring,
    );
    is_deeply(
        $parser->parse( $t->to_xml ),
        $parser->parse($xmlstring),
        "Successfully parses good template: " . ++$c . ' of ' . @goodies
      )
      or diag explain($parser->parse( $t->to_xml )), explain($parser->parse($xmlstring));
    is(
        $t->sort_order,
        $sort_order[ $c - 1 ] . ' asc',
        "Gets the sort order right too"
    );
}

############################################################
# Test ability to throw errors at bad templates
############################################################

while ( my ( $name, $xmlstring ) = each(%baddies) ) {
    my $baddie;
    throws_ok(
        sub {
            $baddie = $module->new(
                service       => $service,
                source_string => $xmlstring,
                model         => $model
            );
        },
        qr/$exp_err_for{$name}/,
        "Raises error parsing bad template: $name",
    ) or diag($baddie->to_xml);
}
