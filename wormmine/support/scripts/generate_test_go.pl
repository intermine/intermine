#!/usr/bin/perl
use strict;
use warnings;

use XML::Simple;
use File::Temp qw/ tempfile /;
#use Data::Dumper;

#Usage: ./generate_test_go.pl Gene.xml go.obo > test-go.obo
#
# Usage is simple: provide the Gene.xml file from the acedb dump and and
# a full GO obo file and the script will output a "go-slim" for testing to STDOUT.
#
# This tool requires that the ROBOT tool be installed:
#    https://github.com/ontodev/robot 

my $genexml = $ARGV[0];
my $goin   = $ARGV[1];

unless ($goin) {
    print "\nUsage: ./generate_test_go.pl <Gene.xml> <go.obo>\n\n";
    exit(0);
}

# make the xml file happy for XML::Simple
my ($fh, $filename) = tempfile(undef, UNLINK => 1);

#according to XML::Simple, you can't have more than one top level tag,
#so I'm putting all of the Gene tags in a silly tag
print $fh "<stuff>\n";

open GENE, $genexml or die $!;
while (<GENE>) {
    print $fh $_ unless $_=~/2_point/; #XML::Simple doesn't appear to like tags that start with numbers
}
print $fh "</stuff>\n";
close GENE;

close($fh);

my $p1 = XMLin($filename);

my %ids;

for (@{$$p1{'Gene'}}) {
    for my $id (@{ $$_{'Gene_info'}{'GO_annotation'}{'GO_annotation'}} ) {
        $ids{"GO:$id"}++;
    }
}


