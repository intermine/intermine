#!/usr/bin/perl -w

# This example shows how a query can be run using just an xml string. 
# You can write these yourself or use the website at flymine.org 
# to create them.

# When viewing a query online, just click the XML button in the bottom right
# you can then cut and paste the XML to use with this service.

use strict;
use warnings;

# We import this module to handle the xml request we provide
use InterMine::WebService::Service::QueryService;

my $service = new InterMine::WebService::Service::QueryService('http://www.flymine.org/query/service', 'service_example');

# This example gets a list of all organisms with their taxon ids and sorts it by the organism name
my $query = q(
   <query name="" model="genomic" view="Organism.name Organism.taxonId" sortOrder="Organism.name"/>
);

# get the results as one string, containing multiple lines and tab delimited columns
my $res = $service->get_result($query); # This returns an HTTP::Request object 
                                        # (see perldoc HTTP::Request

print '-' x 70, "\n" x 2, "all organisms with their taxon ids sorted by the organism name", "\n" x 2;
print $res->content() unless $res->is_error;

