#!/usr/bin/perl

=info
	Creates simple tab delimited file from key mapping csv
	Usage:
	create_mapping.pl <input_file> <output_file> <key_column> <value_column>

	Output file will only contain the "key" and "value" columns 
	in that order.
=cut

my (
	$infile, 
	$outfile,
	$key_col,
	$val_col
	) = @ARGV;

open( IN, $infile) or die $!;
open( OUT, '>'.$outfile) or die $!;

while(<IN>){
	chomp;
	my @line = split(/,/);
	print OUT join("\t",$line[$key_col-1],$line[$val_col-1])."\n";
}
