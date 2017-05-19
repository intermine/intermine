#!/usr/bin/perl

# Reformats fasta file from:
# >seq_id	obj_id ..... 
# to
# >obj_id|seq_id
#
# removes duplicate records sharing obj_id
 
use strict;

my ($infile_path,$outfile_path) = @ARGV;

open( my $infile, $infile_path) or die $infile_path.": $!";
open( my $outfile, '>'.$outfile_path) or die $outfile_path.": $!"; 

my %ive_seen_this = ();

my $chunk = '';
while(<$infile>){
	if(/^>/){
		&process($chunk);
		$chunk = '';
	}
	$chunk .= $_;
}
&process($chunk) if length($chunk) > 0;

sub process{
	my $chunk = shift;
	# format: >seq_id \t obj_id
	# I use obj_id as pid, but duplicates exist
	$chunk =~ s[>(\S+)\s+(\S+).*?\n][>$2|$1\n]; 
	if( !defined($ive_seen_this{$2}) ){
		print $outfile $chunk; 
		$ive_seen_this{$2} = 1;
	}else{
		#print $2."\n";
	}
	
}