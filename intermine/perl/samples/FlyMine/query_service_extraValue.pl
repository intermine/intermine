#!/usr/bin/perl

# This sample shows how to use these modules to call the API,
# returning results as a tab delimited list
#
# This sample demonstrates how to restrict a constraint to
# matching items in a particular organism using the extra_value method

use strict;
use warnings;

# Basic Housekeeping
use InterMine::PathQuery;
use InterMine::WebService::Service::QueryService;
use InterMine::WebService::Service::ModelService;

my $url = 'http://www.flymine.org/query/service';
my $app = 'perlAPI';

my $qs = InterMine::WebService::Service::QueryService->new($url, $app);
my $ms = InterMine::WebService::Service::ModelService->new($url, $app);

my $model = $ms->get_model;
my $pq    = InterMine::PathQuery->new($model);

# Select the visible columns
$pq->add_view("Gene.primaryIdentifier Gene.symbol Gene.organism.name");

# Add a search constraint
my $con = $pq->add_constraint(q/Gene LOOKUP "runt"/);

# Perform the search and retrieve the results
my $res = $qs->get_result($pq);

# Print out the results
print '-' x 70, "\n" x 2, qq(Genes matching "runt"), "\n" x 2;
print $res->content unless $res->is_error;

# Limit the constraint to one particular organism
$con->extra_value('D. melanogaster');

$res = $qs->get_result($pq);

# Print out only the genes in D. Melanogaster
print '-' x 70, "\n" x 2, qq(Genes matching "runt" in "D. Melanogaster"), "\n" x 2;
print $res->content unless $res->is_error;
