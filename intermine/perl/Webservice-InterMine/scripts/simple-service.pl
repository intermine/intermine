#!/usr/bin/perl

use lib 'lib';

use strict;
use warnings;

use Webservice::InterMine::Simple;

@ARGV == 2 or die "Bad args";

my ($url, $xml_file) = @ARGV;

my $service = Webservice::InterMine::Simple->get_service($url);
my $query = $service->new_from_xml(source_file => $xml_file);

print $query->results(as => "string");
