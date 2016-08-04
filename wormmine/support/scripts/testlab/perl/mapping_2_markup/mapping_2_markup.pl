#!/usr/bin/perl

use strict;

=info

Converts mapping files to tables represented in mediawiki markup 

=cut

my ($infilename, $outfilename) = @ARGV;

&usage unless scalar @ARGV == 2;

my ($infile, $outfile);
open( $infile, $infilename) or die;
open( $outfile, '>'.$outfilename) or die;

print $outfile "<tt>\n{| border=\"1\"\n! InterMine field !! XPath to data\n";

while(<$infile>){
	my @token = ();
	if(@token = /(\S+)\s*=\s*(.+)/){
		printf $outfile ("|-\n|%s\n||%s\n",@token);
	}
}

print $outfile "|}\n</tt>\n";

sub usage{
	print 
		"\nUsage: \n$0 <infile> <outfile>\n";
	die "\n";
}
