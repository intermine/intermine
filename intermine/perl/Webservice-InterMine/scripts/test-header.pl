#!/usr/bin/perl

use strict;
use warnings;

use URI;
use LWP;
use Encode;
use feature ':5.10';

@ARGV == 2 or die "Bad args";

my ($url, $xml_file) = @ARGV;

my $uri = URI->new($url . "/query/results");
open (my $qfh, '<', $xml_file) or die;
my $xml = join('', <$qfh>);
close $qfh or die;

say '=' x 20;

for my $ah (0, 1, "path") {
		$uri->query_form(query => $xml, format => "csv", columnheaders => $ah, size => 5);

		my $ua = LWP::UserAgent->new;

		my $result = $ua->get($uri);
		say encode_utf8($result->content);
}
