#!/usr/bin/env perl

package Nuc_translator;

use strict;
require Exporter;
our @ISA = qw (Exporter);
our @EXPORT = qw (translate_sequence get_protein reverse_complement);

my %codon_table;

sub translate_sequence {
    my ($sequence, $frame) = @_;
    $sequence = uc ($sequence);
    $sequence =~ tr/T/U/;
    my $seq_length = length ($sequence);
    unless ($frame > 0 and $frame < 4) { $frame = 1;}
    my $start_point = $frame - 1;
    my $protein_sequence;
    for (my $i = $start_point; $i < $seq_length; $i+=3) {
	my $codon = substr($sequence, $i, 3);
	my $amino_acid;
	if (exists($codon_table{$codon})) {
	    $amino_acid = $codon_table{$codon};
	} else {
	    if (length($codon) == 3) {
		$amino_acid = 'X';
	    } else {
		$amino_acid = "";
	    }
	}
	$protein_sequence .= $amino_acid;
    }
    return($protein_sequence);
}

sub get_protein {
    my ($sequence) = @_;
    ## Assume frame 1 unless multiple stops appear.
    my $least_stops = undef();
    my $least_stop_prot_seq = "";
    foreach my $forward_frame (1, 2, 3) {
	my $protein = &translate_sequence($sequence, $forward_frame);
	my $num_stops = &count_stops_in_prot_seq($protein);
	if ($num_stops == 0) {
	    return ($protein);
	} else {
	    if (!defined($least_stops)) {
		#initialize data
		$least_stops = $num_stops;
		$least_stop_prot_seq = $protein;
	    } elsif ($num_stops < $least_stops) {
		$least_stops = $num_stops;
		$least_stop_prot_seq = $protein;
	    } else {
		#keeping original $num_stops and $least_stop_prot_seq
	    }
	}
    }
    return ($least_stop_prot_seq);
}

sub reverse_complement {
    my($s) = @_;
    my ($rc);
    $rc = reverse ($s);
    $rc =~tr/ACGTacgtyrkmYRKM/TGCAtgcarymkRYMK/;
    return($rc);
}


####
sub count_stops_in_prot_seq {
    my ($prot_seq) = @_;
    chop $prot_seq; #remove trailing stop.
    my $stop_num = 0;
    while ($prot_seq =~ /\*/g) {
	$stop_num++;
    } 
    return ($stop_num);
}


%codon_table = (    UUU => 'F',
		    UUC => 'F',
		    UUA => 'L',
		    UUG => 'L',

		    CUU => 'L',
		    CUC => 'L',
		    CUA => 'L',
		    CUG => 'L',

		    AUU => 'I',
		    AUC => 'I',
		    AUA => 'I',
		    AUG => 'M',
		    
		    GUU => 'V',
		    GUC => 'V',
		    GUA => 'V',
		    GUG => 'V',

		    UCU => 'S',
		    UCC => 'S',
		    UCA => 'S',
		    UCG => 'S',

		    CCU => 'P',
		    CCC => 'P',
		    CCA => 'P',
		    CCG => 'P',
		    
		    ACU => 'T',
		    ACC => 'T',
		    ACA => 'T',
		    ACG => 'T',

		    GCU => 'A',
		    GCC => 'A',
		    GCA => 'A',
		    GCG => 'A',
		    
		    UAU => 'Y',
		    UAC => 'Y',
		    UAA => '*',
		    UAG => '*',
		    
		    CAU => 'H',
		    CAC => 'H',
		    CAA => 'Q',
		    CAG => 'Q',
		    
		    AAU => 'N',
		    AAC => 'N',
		    AAA => 'K',
		    AAG => 'K',
		    
		    GAU => 'D',
		    GAC => 'D',
		    GAA => 'E',
		    GAG => 'E',
		    
		    UGU => 'C',
		    UGC => 'C',
		    UGA => '*',
		    UGG => 'W',

		    CGU => 'R',
		    CGC => 'R',
		    CGA => 'R',
		    CGG => 'R',

		    AGU => 'S',
		    AGC => 'S',
		    AGA => 'R',
		    AGG => 'R',
		    
		    GGU => 'G',
		    GGC => 'G',
		    GGA => 'G',
		    GGG => 'G'    );






1; #end of module
