#!/usr/bin/perl
use strict;

my $file = shift;
my $output_file = 'c_elegans.6239.current.annotations.gff3';

open IN,"/bin/gunzip -c $file |";
my %data;

while (<IN>) {
    next if /^#/;      # ignore comments
    s/^CHROMOSOME_//i; # remove chromosome identifiers
    s/^chr//i;         #
    my $type = get_type($_);
    my $source = get_source($_);
    next if $source eq 'history';
    $data{$type} = () unless( exists $data{$type} );
    push(@{$data{$type}}, $_);
}
close IN;
system("mv $file $file.original.gz");

#    my @order = qw(gene mRNA exon CDS); 
my @order = qw(gene rRNA_primary_transcript nc_primary_transcript miRNA_primary_transcript primary_transcript transposable_element_insertion_site transposable_element tRNA pseudogenic_transcript snoRNA snRNA miRNA mRNA exon CDS three_prime_UTR five_prime_UTR complex_substitution substitution SNP deletion insertion_site operon);
my @types = keys %data;

foreach my $desired_order (@order) {
    foreach my $line (@{$data{$desired_order}}) {
	chomp $line;
	my ($ref,$source,$type,$start,$stop,$score,$strand,$phase,$attributes) = split("\t",$line);
	
	# Let's only keep WormBase features for now.
	# Kev's prototypical GFF3 has source of Wormbase.
	# but my GFF2->GFF3 does not.
	# next unless $source eq 'WormBase';
	
	# Mangle our attributes, removing the prefixes that will break integration.
	$attributes =~ s/Name=Gene:/Name=/g;
#		$attributes =~ s/ID=RGD/ID=/g;
#		$attributes =~ s/Parent=RGD/Parent=/g;
	$attributes =~ s/=\s+/=/g;
	$attributes =~ s/,\t/,/g;
	$attributes =~ s/\s+?;/;/g;
#		$attributes =~ s/associatedGene[\d\D]+?ID/ID/g;
#		$attributes =~ s/\t//g;
#		$attributes =~ s/\w+=;//g;
#		$attributes =~ s/,\s+/,/g;
	
	print join("\t",$ref,$source,$type,$start,$stop,$score,$strand,$phase,$attributes),"\n";
    }
}
#    system("sort -k1,1 -k3,3n -k4,4n -T /tmp tmp.gff > c_elegans.current.annotations.sorted.gff3");
#    system("gzip c_elegans.current.annotations.sorted.gff3");
#    unlink "tmp.gff";


sub get_type {
    my $line = shift;
    my @data = split(/\t/,$line);
    return $data[2];
}






