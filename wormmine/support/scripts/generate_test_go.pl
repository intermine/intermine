#!/usr/bin/perl
use strict;
use warnings;

use XML::Simple;
use File::Temp qw/ tempfile /;
#use Data::Dumper;

#Usage: ./generate_test_go.pl Gene.xml go.owl 
#
# Usage is simple: provide the Gene.xml file from the acedb dump and and
# a full GO owl file and the script will output a file called go-test.owl.
#
# This tool requires that the ROBOT tool be installed:
#    https://github.com/ontodev/robot 

my $genexml = $ARGV[0];
my $goin   = $ARGV[1];

unless ($goin) {
    print "\nUsage: ./generate_test_go.pl <Gene.xml> <go.owl>\n\n";
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
        #my $tmp = "$id";
        $id =~ s/^0//;
        $ids{"GO:$id"}++;
    }
}

# construct a command for robot to make the go slim

#my @goterms;
#for my $key (keys %ids) {
#    push @goterms, "--lower-term $key";
#}

#my $terms = join(" ",@goterms);

#open OUT, ">go-terms.txt";
#for my $key (keys %ids) {
#    print OUT "$key\n"; 
#}
#close OUT;

my ($gofh, $gofilename) = tempfile(undef, UNLINK => 0);

for my $key (keys %ids) {
    print $gofh "$key\n";
}
close ($gofh);

my $robot   = "/home/scain/robot/bin/robot";
my $command = "$robot extract --method BOT --input /home/scain/robot/go.owl --term-file $gofilename --output go-test.obo";

system($command);

exit(0);

