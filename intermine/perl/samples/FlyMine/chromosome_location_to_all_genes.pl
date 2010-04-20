#!/usr/bin/perl

### Using the Template webservice to query FlyMine
# This example shows you how you can extend simple Template Queries
# and make stand alone scripts with rich functionality 
# to automate access to your favourite templates from your
# own machine
#
# You can use this script to show the genes located between 
# two points on a chromosome. (Data Source: FlyBase, Ensembl).
#
# This example makes use of the Chromosomal location => All genes Template
# url: http://www.flymine.org/release-24.0/template.do?name=ChromLocation_Gene

use strict;
use warnings;
use LWP::Simple;
use URI;

# Set up default values
# Default value for primary identifier is '2L'
my $primary_identifier       = '2L';                     
# Default value for organism is 'Drosophila Melanogaster'
my $organism                 = 'Drosophila melanogaster';
my $start_location           = 1;         # Start at the beginning
my $end_location             = 50_000;    # Default value is 50,000
my $no_of_results_to_return  = 20;        # Just return 20 results.

# The Template URL - visit http://www.flymine.org/query/templates.do to see more
my $serviceurl = 'http://www.flymine.org/release-24.0/service/template/results?name=ChromLocation_Gene&constraint1=Chromosome.primaryIdentifier&op1=eq&value1=SEARCHTERM0&constraint2=Chromosome.organism.name&op2=eq&value2=SEARCHTERM1&constraint3=Chromosome.genes.chromosomeLocation.start&op3=gt&value3=SEARCHTERM2&constraint4=Chromosome.genes.chromosomeLocation.end&op4=lt&value4=SEARCHTERM3&size=SEARCHTERM4&format=tab';

my $i = 0;
for ($primary_identifier, 
     $organism, 
     $start_location, 
     $end_location, 
     $no_of_results_to_return) {
     # This escapes the replacements to use in a url
    my $uri_fragment = URI->new($_);          
     # This inserts our search terms into the url
    $serviceurl =~ s/SEARCHTERM$i/$uri_fragment/g;  
     # Increment the counter to select a different term
    $i++;                                     
}

# Announce what we have found
print '-' x 70, "\n" x 2, "First $no_of_results_to_return genes between $start_location and $end_location on the $primary_identifier chromosome in $organism", "\n" x 2;

my $output = get($serviceurl);   # This gets the document of results.
$output =~ s/\r//g;              # strip carriage returns
print $output;
