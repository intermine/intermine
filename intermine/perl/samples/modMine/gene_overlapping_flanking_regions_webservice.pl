#!/usr/bin/perl

### Using the Template webservice to query modMine
# This example shows you how you can extend simple Template Queries
# and make stand alone scripts with rich functionality 
# to automate access to your favourite templates from your
# own machine
#
# This example makes use of the gene_overlapping_flanking_region Template
# url: http://intermine.modencode.org/query/service/template/results?name=gene_overlapping_flanking_region

use strict;
use warnings;
use LWP::Simple;
use URI;

# You can use Getopt::Long to make your searchterms into commandline arguments
# See perldoc Getopt::Long for its many options.
use Getopt::Long;

# Set up default values
my $GeneFlankingRegion          = 'eve';
my $GeneFlankingRegionDistance  = '0.5kb'; # possible values are 0.5, 1.0kb, 2.0kb, 5.0kb, 10.0kb
my $Up_Down_stream              = 'upstream';
my $no_of_results_to_return     = 20;

my $result = GetOptions(
         # These are string values
    "FlankingRegion=s"                   => \$GeneFlankingRegion,
    "Distance=s"                         => \$GeneFlankingRegionDistance,
         # This is a toggle value
    "upstream"                           => sub {$Up_Down_stream = 'upstream'}, 
    "downstream"                         => sub {$Up_Down_stream = 'downstream'},
         # This one takes an int
    "size|no_of_results_to_return=i"     => \$no_of_results_to_return,
    );

# Check the sanity of GeneFlankingDistance
unless ( $GeneFlankingRegionDistance =~ /(0\.5|(1|2|5|10)\.0)kb/ ) {
    die "The only valid Flanking distances are 0.5, 1.0kb, 2.0kb, 5.0kb, and 10.0kb.\n";
}

# The Template URL - visit http://intermine.modencode.org/query/templates.do to see more
my $url = 'http://intermine.modencode.org/query/service/template/results?name=gene_overlapping_flanking_regions&constraint1=GeneFlankingRegion.overlappingFeatures.featureType&op1=eq&value1=*binding*&constraint2=GeneFlankingRegion.distance&op2=eq&value2=SEARCHTERM1&constraint3=GeneFlankingRegion.gene&op3=LOOKUP&value3=SEARCHTERM0&extra3=&constraint4=GeneFlankingRegion.direction&op4=eq&value4=SEARCHTERM2&size=SEARCHTERM3&format=tab';


my $i = 0;
for ($GeneFlankingRegion, $GeneFlankingRegionDistance, $Up_Down_stream, $no_of_results_to_return) {
    my $uri_fragment = URI->new($_);          # This escapes the replacements to use in a url
    $url =~ s/SEARCHTERM$i/$uri_fragment/g;
    $i++;
}

print '-' x 70, "\n" x 2, "First $no_of_results_to_return genes (and their antibody targets) that overlap, $GeneFlankingRegionDistance $Up_Down_stream of $GeneFlankingRegion.", "\n" x 2;

my $res = getprint($url);                           # This gets the document, and prints it out.
