#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 8;
use Test::Exception;

{
    package TestPathHolder;
    use Moose;

    use InterMine::Model::Types qw/PathString PathList/;

    has path => (
        is => 'ro',
        isa => PathString,
    );

    has paths => (
        is => 'ro', 
        isa => PathList,
        coerce => 1,
    );

    no Moose;
}

my $test_a = TestPathHolder->new(path => "Some.path.here", paths => [qw/Some.paths.here And.here/]);

is $test_a->path, "Some.path.here";
is_deeply $test_a->paths, [qw/Some.paths.here And.here/];

dies_ok {TestPathHolder->new(path => "A bad path")};
dies_ok {TestPathHolder->new(paths => "(*&^%^%")};

my $test_b = TestPathHolder->new(paths => "Some.paths.here And.here");
is_deeply $test_b->paths, [qw/Some.paths.here And.here/];

my $test_c = TestPathHolder->new(paths => "Some.paths.here, And.here");
is_deeply $test_c->paths, [qw/Some.paths.here And.here/];

my $test_d = TestPathHolder->new(path => "A.path_with_underscores");
is $test_d->path, "A.path_with_underscores";

my $test_e = TestPathHolder->new(path => "Protein.UniProt_accession");
is $test_e->path, "Protein.UniProt_accession", "Should be OK with J. Blackshaw's path";


