#!/usr/bin/perl

# Generate species filter from wormbase.conf <all_species>

use strict;
use XML::Simple qw(:strict);
use Data::Dumper;

my $infile_path = 'wormbase.conf';
my $outfile_path = 'species_whitelist.txt';

open( my $infile, $infile_path) or die $infile_path.": $!";

my $xs = XML::Simple->new( 
	ForceArray 	=> 1, 
	KeyAttr 	=> {},
	ForceContent => 1
);

my $xml='';
while(<$infile>){
	next if /^\s+notes/;
	$xml .= $_ unless $xml eq '';
	$xml .= $_ if /^<species_list>/;
	last if m[^</species_list>];
}

#print $xml;die;

my $ref = $xs->XMLin($xml);

my @full_name = ();
foreach my $species ( keys %$ref ){
	next unless ref $ref->{$species} eq 'ARRAY';
	
	my $species_conf = $ref->{$species}[0]{content}; 
	if(ref $species_conf eq 'ARRAY'){
		$species_conf = $species_conf->[0] 
	}
	my @genus_species = 
		$species_conf =~ /genus\s+(\S+)[\s\n]+species\s+"(.+?)"/ ?
		$species_conf =~ /genus\s+(\S+)[\s\n]+species\s+"(.+?)"/ :
		$species_conf =~ /genus\s+(\S+)[\s\n]+species\s+(\S+)/;
	my $species_name = join( " ", @genus_species);
	push @full_name, $species_name unless $species_name eq ''; 
	
}

open( my $outfile, '>'.$outfile_path) or die $outfile_path.": $!";
print $outfile join("\n", @full_name);

print scalar Dumper @full_name;
