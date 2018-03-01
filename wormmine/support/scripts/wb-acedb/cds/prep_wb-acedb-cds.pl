#!/usr/bin/perl

use strict;

my ($infile_path,$outfile_path) = @ARGV;

open( my $infile, $infile_path) or die $infile_path.": $!";
open( my $outfile, '>'.$outfile_path) or die $outfile_path.": $!";

while(<$infile>){
#	s[^<CDS>(.+?)(\s*)$][<CDS>CDS:$1$2]; # covers this in mapping file
	s[<Transcript>(.+?)</Transcript>][<Transcript>Transcript:$1</Transcript>];
	print $outfile $_;
}