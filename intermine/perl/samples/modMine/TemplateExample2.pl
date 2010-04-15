#!/usr/bin/perl

use strict;
use warnings;
use LWP::Simple;
use URI;

my @args = @ARGV;

my $url = 'http://intermine.modencode.org/query/service/template/results?name=Array_Submission&constraint1=Array.name&op1=eq&value1=SEARCHTERM&size=10&format=tab';

my $searchterm = ($args[0] || 'Affymetrix Drosophila Tiling Arrays v2.0R'); # Array

my $uri_fragment = URI->new($searchterm); # This escapes the searchterm to use in a url
$url =~ s/SEARCHTERM/$uri_fragment/g;

my $res = getprint($url);                  # This gets the document, and prints it out.

