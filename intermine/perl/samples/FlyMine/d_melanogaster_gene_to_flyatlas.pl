#!/usr/bin/perl

### Using the Template webservice to query FlyMine
# This example shows you how you can extend simple Template Queries
# and make stand alone scripts with rich functionality 
# to automate access to your favourite templates from your
# own machine
#
# You can use this script to show expression data from FlyAtlas
# for a given D. melanogaster gene.
#
# This example makes use of the Gene [D. melanogaster] => FlyAtlas data template
# http://www.flymine.org/release-24.0/template.do?name=Gene_FlyAtlas

use strict;
use warnings;
use LWP::Simple;
use URI;

# Set up default values
my $gene                     = 'FBgn0011582'; # Default value is 'FBgn0011582'
my $no_of_results_to_return  = 20;            # Just return 20 results.

# The Template URL - visit http://www.flymine.org/query/templates.do to see more
my $serviceurl = 'http://www.flymine.org/release-24.0/service/template/results?name=Gene_FlyAtlas&constraint1=Gene&op1=LOOKUP&value1=SEARCHTERM0&extra1=&size=SEARCHTERM1&format=tab';

my $i = 0;
for ($gene, $no_of_results_to_return) {
     # This escapes the replacements to use in a url
    my $uri_fragment = URI->new($_);          
     # This inserts our search terms into the url
    $serviceurl =~ s/SEARCHTERM$i/$uri_fragment/g;  
     # Increment the counter to select a different term
    $i++;                                     
}

# Announce what we have found
print '-' x 70, "\n" x 2, "First $no_of_results_to_return FlyAtlas entries for $gene", "\n" x 2;

my $output = get($serviceurl);   # This gets the document of results.
$output =~ s/\r//g;              # strip carriage returns
print $output;

exit;
