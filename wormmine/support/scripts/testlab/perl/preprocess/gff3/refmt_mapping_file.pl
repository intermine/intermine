#!/usr/bin/perl

use strict;

my ($infile_path, $outfile_path) = @ARGV;
&usage unless scalar @ARGV == 2;

open( my $infile, $infile_path) or die $infile_path.": $!";
open( my $outfile, '>'.$outfile_path) or die $outfile_path.": $!";

while(<$infile>){
	chomp;
	my @tokens = split /,/;
	if( $tokens[1] ne $tokens[3] && $tokens[3] ne ''){
		print $outfile join("\t",@tokens[3,1])."\n";
	}
}

sub usage{
print <<USAGE;
Usage: $0 infile outfile

converts file of format:
a,b,c,d,e
to
d	b ( <- tab )

Intended for wormbase gff3 id mapping files
USAGE
die "\n";
}