#!/usr/bin/perl

### Using the Template webservice to query FlyMine
# This example shows you how you can extend simple Template Queries
# and make stand alone scripts with rich functionality 
# to automate access to your favourite templates from your
# own machine
#
# This example makes use of the Gene => GO terms Template
# url: http://www.flymine.org/release-24.0/template.do?name=Gene_GO

use strict;
use warnings;
use LWP::Simple;
use URI;

sub check {

    my ($class, $instance)  = @_;

    my $url = "http://www.flymine.org/query/service/query/results?query=%3Cquery+name%3D%22%22+model%3D%22genomic%22+view%3D%22${class}%22+sortOrder%3D%22${class}+asc%22%3E%3C%2Fquery%3E&format=tab";
    my $list = get($url);
    $list =~ s/\r//g;
    my @instances = split("\n", $list);
    
    unless (grep /^$instance$/, @instances) {
	warn "$instance is not a valid $class - it must be one of:\n" .
	     join(",\n", @instances) .
	     "\n";
	return;
    }
    else {
	return 1;
    }
}

# You can use Getopt::Long to make your searchterms into commandline arguments
# See perldoc Getopt::Long for its many options.
use Getopt::Long;

# Set up default values
# Default value for primary identifier is '2L'
my $primary_identifier       = '2L';                     
# Default value for organism is 'Drosophila Melanogaster'
my $organism                 = 'Drosophila melanogaster';
my $start_location           = 1;         # Start at the beginning
my $end_location             = 50_000;    # Default value is 50,000
my $no_of_results_to_return  = 20;        # Just return 20 results.

my $result = GetOptions(
         # These are string values,
    "primaryid=s"      => \$primary_identifier,
    "organism=s"       => \$organism,
         # These take integers.
    "start_location=i" => \$start_location,
    "end_location=i"   => \$end_location,
    "size|no_of_results_to_return=i"     => \$no_of_results_to_return,
    );

# Check the sanity of $primary_identifier and $organism.
my %class_for = ($primary_identifier => 'Chromosome.primaryIdentifier',
		 $organism           => 'Organism.name',
    );

for my $var (keys %class_for) {
    die "Aborting\n" unless (check($class_for{$var}, $var));
}

# The Template URL - visit http://www.flymine.org/query/templates.do to see more
my $serviceurl = 'http://www.flymine.org/release-24.0/service/template/results?name=ChromLocation_Gene&constraint1=Chromosome.primaryIdentifier&op1=eq&value1=SEARCHTERM0&constraint2=Chromosome.organism.name&op2=eq&value2=SEARCHTERM1&constraint3=Chromosome.genes.chromosomeLocation.start&op3=gt&value3=SEARCHTERM2&constraint4=Chromosome.genes.chromosomeLocation.end&op4=lt&value4=SEARCHTERM3&size=SEARCHTERM4&format=tab';

my $i = 0;
for ($primary_identifier, $organism, $start_location, $end_location, $no_of_results_to_return) {
    my $uri_fragment = URI->new($_);          # This escapes the replacements to use in a url
    $serviceurl =~ s/SEARCHTERM$i/$uri_fragment/g;   # This inserts our search terms into the url
    $i++;                                     # Increment the counter to select a different term
}

# Announce what we have found
print '-' x 70, "\n" x 2, "First $no_of_results_to_return genes between $start_location and $end_location on the $primary_identifier chromosome in $organism", "\n" x 2;

my $output = get($serviceurl);                     # This gets the document of results.
$output =~ s/\r//g; # strip new lines
print $output;
