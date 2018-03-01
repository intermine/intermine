#!/usr/bin/perl

use strict;

my ($infile_path,$outfile_path) = @ARGV;

open( my $infile, $infile_path) or die $infile_path.": $!";
open( my $outfile, '>'.$outfile_path) or die $outfile_path.": $!";

while(<$infile>){
	#s[<Transcript>(.+?)(\s*)$][<Transcript>Transcript:$1$2];  # covered by concat() xpath func
	s[<CDS>(.+?)][<CDS>CDS:$1];
	print $outfile $_;
}
