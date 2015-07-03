#!/usr/bin/perl

use strict;
use warnings;

use JSON::Schema;
use LWP::Simple;
use JSON;
use Data::Dumper;
use Carp;

$SIG{__DIE__} = \&Carp::confess; 

@ARGV == 2 or die <<"USAGE";
Simple Validator Script

Usage: $0 schema_url url_to_validate
USAGE

my ($schema_url, $url_to_validate) = @ARGV;

my $schema = get($schema_url);
my $to_validate = get($url_to_validate); 

my $decoder = JSON->new();

my $validator = JSON::Schema->new($decoder->decode($schema)); 
my $json = $decoder->decode($to_validate); 

my $result = $validator->validate($json); 

if ($result) {
    print "Valid\n";
} else {
    print "Errors\n"; 
    print " - $_\n" for $result->errors;
}

