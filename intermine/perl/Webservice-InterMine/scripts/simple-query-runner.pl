#!/usr/bin/perl

use strict;
use warnings;

use URI;
use LWP;
use Encode;

@ARGV == 2 or die "Bad args";

my ($url, $xml_file) = @ARGV;

my $uri = URI->new($url . "/query/results");
open (my $qfh, '<', $xml_file) or die;
my $xml = join('', <$qfh>);
close $qfh or die;

$uri->query_form(query => $xml, format => "tab");

my $ua = LWP::UserAgent->new;

my $result = $ua->get($uri);
print encode_utf8($result->content);
