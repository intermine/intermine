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

# You can use Getopt::Long to make your searchterms into commandline arguments
# See perldoc Getopt::Long for its many options.
use Getopt::Long;

# Set up default values
my $gene                     = 'CG11348'; # Default value is 'CG11348'
my $organism                 = '';        # Default value is 'any'
my $no_of_results_to_return  = 20;        # Just return 20 results.

my $result = GetOptions(
         # These are string values,
    "gene=s"     => \$gene,
    "organism=s" => \$organism,
         # This one takes an integer.
    "size|no_of_results_to_return=i"     => \$no_of_results_to_return,
    );

# Check the sanity of $organism
my $org_list = get('http://www.flymine.org/query/service/query/results?query=%3Cquery+name%3D%22%22+model%3D%22genomic%22+view%3D%22Organism.shortName%22+sortOrder%3D%22Organism.shortName+asc%22%3E%3C%2Fquery%3E&format=tab'); # Get a list of organisms, using the shortName format
$org_list =~ s/\r//g; # Delete carriage returns; a hacky solution, but sadly necessary

if ($org_list =~ /melanogaster/) { 
    # Make sure our $organism is one of the ones we got from the webservice
    my @organisms = split "\n", $org_list;
    unless ( (! $organism) || (grep /^$organism$/, @organisms) ) {
	die "'$organism' is not a valid organism - it must be one of:\n" . 
            join (",\n", @organisms) .
	    "\n";
    }
}
else {  # if D. melanogaster ain't there, it probably didn't work - thus no point checking
    warn "We didn't fetch a good list of organisms - we can't be sure $organism will work!\n";
}

# The Template URL - visit http://www.flymine.org/query/templates.do to see more
my $url = 'http://www.flymine.org/query/service/template/results?name=Gene_GO&constraint1=Gene&op1=LOOKUP&value1=SEARCHTERM0&extra1=SEARCHTERM1&size=SEARCHTERM2&format=tab';

my $i = 0;
for ($gene, $organism, $no_of_results_to_return) {
    my $uri_fragment = URI->new($_);          # This escapes the replacements to use in a url
    $url =~ s/SEARCHTERM$i/$uri_fragment/g;   # This inserts our search terms into the url
    $i++;                                     # Increment the counter to select a different term
}

# Announce what we have found
print '-' x 70, "\n" x 2, "First $no_of_results_to_return GO annotations for $gene", ($organism) ? " in $organism" : '', "\n" x 2; 

my $output = get($url);                     # This gets the document of results.
$output =~ s/\r//g; # strip new lines
print $output;
