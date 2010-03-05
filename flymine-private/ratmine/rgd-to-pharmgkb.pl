#!/usr/bin/perl
# rgd-to-pharmgkb.pl
# by Andrew Vallejos

use warnings;
use strict;

BEGIN {
  push (@INC, ($0 =~ m:(.*)/.*:)[0] . '../intermine/perl/lib');
}

use XML::Writer;
use InterMine::Item;
use InterMine::ItemFactory;
use InterMine::Model;
use InterMine::Util qw(get_property_value);
use IO qw(Handle File);
use Cwd;
use Getopt::Long;
use strict;

my ($ortho_file, $pharm_file, $help, $out_file, $model_file);
my $t = 0;

GetOptions( 'model=s' => \$model_file,
			'orthologue_file=s' => \$ortho_file,
			'pharmGKB_file=s' => \$pharm_file,
			'output_file=s' => \$out_file,
			'help' => \$help);

if($help or !($ortho_file and $pharm_file))
{
	&printHelp;
	exit(0);
}

# Begin with setting up the environment
my $output = new IO::File(">$out_file");
my $writer = new XML::Writer(DATA_MODE => 1, DATA_INDENT => 3, OUTPUT => $output);

my $model = new InterMine::Model(file => $model_file);
my $item_factory = new InterMine::ItemFactory(model => $model);

my $taxon = '10116';
my $org_item = $item_factory->make_item('Organism');
$org_item->set('taxonId', $taxon);

#build rgd to human index
open(RGD, $ortho_file);
my %index;
my %rgd_mapping;

$writer->startTag("items");
$org_item->as_xml($writer);

#build RGD to Human Orthologue Index
print "Reading Orthologue File...\n";
while(<RGD>)
{
	next if $_ =~ /^#/;
	chomp;
	if($_ =~ /^RAT/)
	{
		my @header = split(/\t/, $_);
		%index = &buildHeaderIndex(\@header);
	}
	else
	{
		my @line = split(/\t/, $_);
		my $rgd_id = $line[$index{RAT_GENE_RGD_ID}];
		my $gene_id = $line[$index{HUMAN_ORTHOLOG_ENTREZ}];
		my $symbol = $line[$index{RAT_GENE_SYMBOL}];
		foreach my $g (split(/\|/, $gene_id)) {
			$rgd_mapping{$g}{id} = $rgd_id;
			$rgd_mapping{$g}{symbol} = $symbol;
		}
	}
}#end while <RGD>
close RGD;

print "Closing Orthologue File...\nOpening Pharm File...\n";

%index = ();
open(PKB, $pharm_file);
my %pharm_items;
my %rgd_flags;
while(<PKB>)
{
	chomp;
	if($_ =~ /^Pharm/)
	{
		my @header = split(/\t/, $_);
		%index = &buildHeaderIndex(\@header);
	}
	else
	{
		my @line = split(/\t/, $_);
		my $pharm_id = $line[$index{'PharmGKB Accession Id'}];
		my $gene_id = $line[$index{'Entrez Id'}];
		unless (exists($rgd_flags{ $rgd_mapping{$gene_id}{id} }))
		{
			if($rgd_mapping{$gene_id}{id})
			{
				#push(@{$rgd_mapping{$gene_id}{pharmids}}, $pharm_id);
				my $gene_item = $item_factory->make_item('Gene');
				$gene_item->set('primaryIdentifier', $rgd_mapping{$gene_id}{id});
				$gene_item->set('pharmGKBidentifier', $pharm_id);
				$gene_item->set('organism', $org_item);
				$gene_item->as_xml($writer);
				$rgd_flags{ $rgd_mapping{$gene_id}{id} } = 'true';
				#print !exists($rgd_mapping{$gene_id}{flag}) ."\n"; exit(0);
			}
		}
		#print "$pharm_id\t$gene_id\t$rgd_mapping{$gene_id}{id}\t$rgd_mapping{$gene_id}{symbol}\n";

	} #else
}#end while <PKB>

=cut

foreach my $gid (keys %rgd_mapping)
{
	if($rgd_mapping{$gid}{id})
	{
		#print "$pid\n";
		#$t++;
		my $gene_item = $item_factory->make_item('Gene');
		$gene_item->set('primaryIdentifier', $rgd_mapping{$gid}{id});
		my @pharm_items;
		foreach my $pid (@{$rgd_mapping{$gid}{pharmids}})
		{
			if($pid)
			{
				my $pharm_item = $item_factory->make_item('PharmGKB');
				$pharm_item->set('primaryIdentifier', $pid);
				$pharm_item->set('gene', $gene_item);
				$pharm_item->as_xml($writer);
				push(@pharm_items, $pharm_item);
			}#unless
		}
		$gene_item->set('pharmgkbs', \@pharm_items);
		$gene_item->as_xml($writer);
		$gene_item->destroy;
	}#unless
} #foreach my $pid

=cut

#my @test = keys(%pharm_items);
#print @test . "\n";
#print $test[1] . "\n";
#print "$t\n";
#print "@{$pharm_items{$test[1]}} \n";
close PKB;
print "Closing PharmFile...\n";
$writer->endTag("items");
exit(0);

#####Subroutines########

sub printHelp
{
	print "help\n";
}#end printHelp

sub buildHeaderIndex
{
	my %index;
	my $header = shift;
	my $count = 0;
	foreach my $h (@$header) {
		$index{$h} = $count;
		$count++;
	}
	return %index;
}