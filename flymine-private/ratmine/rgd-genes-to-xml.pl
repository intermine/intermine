#!/usr/bin/perl
# rgd-genes-to-xml.pl
# purpose: to create a target items xml file for intermine from RGD FTP file

#use warnings;
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

my ($model_file, $genes_file, $gene_xml) = @ARGV;

die "Must point to valid InterMine Model" unless (-e $model_file);
my $data_source = 'Rat Genome Database';
my $taxon_id = 10116;
my $output = new IO::File(">$gene_xml");
my $writer = new XML::Writer(DATA_MODE => 1, DATA_INDENT => 3, OUTPUT => $output);

# The item factory needs the model so that it can check that new objects have
# valid classnames and fields
my $model = new InterMine::Model(file => $model_file);
my $item_factory = new InterMine::ItemFactory(model => $model);
$writer->startTag("items");

####
#User Additions
my $org_item = $item_factory->make_item('Organism');
$org_item->set('taxonId', $taxon_id);
$org_item->as_xml($writer);
my $dataset_item = $item_factory->make_item('DataSet');
$dataset_item->set('title', $data_source);
$dataset_item->as_xml($writer);

# read the genes file
open GENES, $genes_file;
my %index;
my %pubs;
while(<GENES>)
{
	chomp;
	if( $_ !~ /^\d/) #parses header line
	{
		my @header = split(/\t/, $_);
		for(my $i = 0; $i < @header; $i++)
		{	$index{$header[$i]} = $i;	}
	
	}
	else
	{
    #    print "\n   ------------ Line: ".$count."  --------------  \n";
		$_ =~ s/\026/ /g; #replaces 'Syncronous Idle' (Octal 026) character with space
		my @gene_info = split(/\t/, $_);
		my @synonym_items;
		my $gene_item = $item_factory->make_item('Gene');
		$gene_item->set('organism', $org_item);
		$gene_item->set('dataSets', [$dataset_item]);
		$gene_item->set('primaryIdentifier', $gene_info[$index{GENE_RGD_ID}]);
		$gene_item->set('secondaryIdentifier', $gene_info[$index{GENE_RGD_ID}]); #add RGD: to number
		$gene_item->set('symbol', $gene_info[$index{SYMBOL}]);
		$gene_item->set('name', $gene_info[$index{NAME}]) unless ($gene_info[$index{NAME}] eq '');	
		$gene_item->set('description', $gene_info[$index{GENE_DESC}]) unless ($gene_info[$index{GENE_DESC}] eq '');
		unless ($gene_info[$index{ENTREZ_GENE}] eq '' or $gene_info[$index{GENE_TYPE}] =~ /splice|allele/i)
		{
			$gene_item->set('ncbiGeneNumber', $gene_info[$index{ENTREZ_GENE}]);
			my $syn_item = $item_factory->make_item('Synonym');
			$syn_item->set('value', $gene_info[$index{ENTREZ_GENE}]);
			$syn_item->set('type', 'ncbiGeneNumber');
			$syn_item->set('subject', $gene_item);
			push(@synonym_items, $syn_item); #set the reverse reference
			$syn_item->as_xml($writer);
		}
		$gene_item->set('geneType', $gene_info[$index{GENE_TYPE}]) unless ($gene_info[$index{GENE_TYPE}] eq '');
		unless ($gene_info[$index{ENSEMBL_ID}] eq '')
		{
			$gene_item->set('ensemblIdentifier', $gene_info[$index{ENSEMBL_ID}]);
			my $syn_item = $item_factory->make_item('Synonym');
			$syn_item->set('value', $gene_info[$index{ENSEMBL_ID}]);
			$syn_item->set('type', 'ensemblIdentifier');
			$syn_item->set('subject', $gene_item);
			push(@synonym_items, $syn_item); #set the reverse reference
			$syn_item->as_xml($writer);
		}
		$gene_item->set('nomenclatureStatus', $gene_info[$index{NOMENCLATURE_STATUS}]) unless ($gene_info[$index{NOMENCLATURE_STATUS}] eq '');
		$gene_item->set('fishBand', $gene_info[$index{FISH_BAND}]) unless ($gene_info[$index{FISH_BAND}] eq '');
    	
		#add synonyms to genes
		$gene_item->set('synonyms', \@synonym_items);
		#process the publications:
    	if ($gene_info[$index{CURATED_REF_PUBMED_ID}] ne '') {
      		#print "Got some pubmed ids: (".$gene_info[$index{CURATED_REF_PUBMED_ID}].")\n";
	      	my @publication_info = split(/,/, $gene_info[$index{CURATED_REF_PUBMED_ID}]);
	      	my @currentPubs = ();
	      	foreach (@publication_info) {
	        #reuse publication object if we already have it in the $pubs array
	        	if (exists $pubs{$_}) {
	          		push(@currentPubs, $pubs{$_});
	        	}
	        	#otherwise, create a new one via the item factory and add it to the $pubs array
	        	else {
	          		my $pub1 = $item_factory->make_item("Publication");
	          		$pub1->set("pubMedId", $_);
	          		$pubs{$_} = $pub1;
	          		$pub1->as_xml($writer);
	          		push(@currentPubs, $pub1);
	        	}#end if-else
	      	}#end foreach
	      	$gene_item->set("publications", \@currentPubs);
    	}#end if
		$gene_item->as_xml($writer);
	} #end if-else	

}#end while
close GENES;
$writer->endTag("items");