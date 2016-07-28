#!/usr/bin/perl
use strict;
use warnings;

use XML::Simple;
use File::Temp qw/ tempfile /;
#use Data::Dumper;

#Usage: ./generate_test_gff.pl Gene.xml c_elegans.PRJNA13758.WS254.annotations.gff3 > test.gff
#
# Usage is simple: provide the Gene.xml file from the acedb dump and and
# the C. elegans GFF file for that release and redirect the output to a new GFF file.
#
# It can take a while to run--about an hour for 5 genes with variations.

my $genexml = $ARGV[0];
my $gffin   = $ARGV[1];

unless ($gffin) {
    print "\nUsage: ./generate_test_gff.pl <Gene.xml> <elegans.gff>\n\n"; 
    exit(0);
}

# make the xml file happy for XML::Simple
my ($fh, $filename) = tempfile(undef, UNLINK => 0);

#according to XML::Simple, you can't have more than one top level tag,
#so I'm putting all of the Gene tags in a silly tag
print $fh "<stuff>\n";


open GENE, $genexml or die $!;
while (<>) {
    print $fh $_ unless $_=~/2_point/; #XML::Simple doesn't appear to like tags that start with numbers  
}
print $fh "</stuff>\n";
close GENE;

close($fh);

my $p1 = XMLin($filename);

my %ids;

for (@{$$p1{'Gene'}}) {
    #print Dumper($_);
    #print $$_{'content'}. "\n";
    my $name = $$_{'content'}; 
    $name =~ s/\s+//g;
    chomp $name;

    $ids{$name}++ if (&uniquename($name));
    $ids{$$_{'Identity'}{'Name'}{'Public_name'}{'Gene_name'}}++ if uniquename($$_{'Identity'}{'Name'}{'Public_name'}{'Gene_name'});
    $ids{$$_{'Identity'}{'Name'}{'Sequence_name'}{'Gene_name'}}++ if uniquename($$_{'Identity'}{'Name'}{'Sequence_name'}{'Gene_name'});

    for my $id (@{ $$_{'Identity'}{'Name'}{'Molecular_name'}{'Gene_name'}  }) {
        $ids{$id}++  if (&uniquename($id));
    }

    for my $var (@{ $$_{'Gene_info'}{'Allele'}{'Variation'} }) {
        my $name = ref $var ? $$var{'content'} : $var;
        $name =~ s/\s+//g;
        chomp $name;
        $ids{$name}++  if (&uniquename($name));;
    }

#    for my $key (keys %ids) {
#        print "$key\n";
#    }
#    die;
}

#print Dumper($p1);
#die;


#while (<>) {
#    my $temp;
#    if         (/<Gene>(\S+?)(<|$)/) {
#        $temp = $1;
#    }
#    elsif (/<Gene_name>(\S+?)(<|$)/) {
#        $temp = $1;
#    }
#    elsif (/<Variation>(\S+?)(<|$)/) {
#        $temp = $1;
#    }

#    next unless $temp;
#    next if $temp =~ /^ENS/;
#    next if $temp =~ /\:/;

#    my $match = 0;
#    for my $key (keys %ids) {
#        if ($temp =~ /\Q$key\E/) {
#            $match = 1;
#            last;
#        }
#    }
#    next if $match;
#
#    $ids{$temp}++;
#}

my %gff;

#yes, grepping through the GFF file can take a while
#about an hour for 5 genes with variation
for my $key (keys %ids) {
    my @out = `grep $key\[^0-9\] $gffin | grep -v RNASEQ.Hillier`;
    if (scalar @out) {
        for (@out) {
            $gff{$_}++;
        }
    }
}

print sort keys %gff;
exit(0);

#my $str = "(";
#$str .= join(")|(", keys %ids);
#$str .= ")";

#$str = "grep -P \"$str\" /media/ephemeral0/scain/c_elegans.PRJNA13758.WS254.annotations.gff3";

#print "$str\n";

#print "\n",length($str);

sub uniquename {
#
# Simple sub to only add new ids to the list if this new id wouldn't be caught
# by an id already in the list (for example, if the sequence name is in the list
# then transcript names would get caught with the sequence name.
#
    my $name = shift;

    return 0 unless $name;
    return 0 if $name =~ /^\s*$/;
    for my $key (keys %ids) {
        if ($name =~ /\Q$key\E/) {
            return 0;
        }
    }
    return 1;
}
