#!/usr/bin/perl -w

#usage: takes data from /shared/data/pubmed/gene2pubmed as $ARGV[0] and writes the xml file
#for linking ncbi gene ID to one or more pubmed IDs. 

use strict;

BEGIN {
  # find the lib directory by looking at the path to this script
  push (@INC, ($0 =~ m:(.*)/.*:)[0] . '/../../intermine/perl/lib/');
}

use IO::File;
use XML::Writer;
use InterMine::Item;
use InterMine::ItemFactory;
use InterMine::Model;

#Kim's stuff for writing XML
my @items = ();
my $model_file = '../../flymine/dbmodel/build/model/genomic_model.xml';
my $model = new InterMine::Model(file => $model_file);
my $item_factory = new InterMine::ItemFactory(model => $model);
my @items_to_write = ();


#define organism ids to include
my %ids=("4932",'S.cerevisiae',
	 "6239",'C.elegans',
	 "7227",'D.melanogaster'#,
	 #"180454",'A.gambiae str PEST'
	 );
my (%genes, %pubID, %pubgene);

#open /shared/data/ncbigene/gene2pubmed and get data
open(F,"<$ARGV[0]") or die "$!";
while(<F>){
	my @f = split/\t/;
	my $current_ID=$f[0];
	#only use the organisms in %ids
	if(exists $ids{$current_ID}){
		chomp $f[2];
		my ($geneID, $pubID) = ($f[1], $f[2]);
		my ($gene_item, $pub_item);
		#print "Gene $geneID pub# $pubID\n";
		
		#check to see if the gene object has already been stored, if not create it
		if(!exists $genes{$geneID}){
			$gene_item = make_item('Gene');
			$gene_item->set('ncbiGeneNumber', $geneID);
			$genes{$geneID}={'object' => $gene_item};
		}
		#check to see if the publication object has already been stored, if not create it
		if(!exists $pubID{$pubID}){
			$pub_item = make_item('Publication');
			$pub_item->set('pubMedId', $pubID);	
			$pubID{$pubID}={'object' => $pub_item};
		}
		#associate the publication with the gene in the hash 
		$genes{$geneID}{'publications'}{$pubID} = $pubID;
	}
}	
close(F) or die "$!";

#for each gene, find which pubmedIds reference it, retrieve objects from the hash
foreach my $gene (sort keys %genes){
	my @pub_items;
	#print "Gene $gene has publications\n";
	my $gene_item = $genes{$gene}->{'object'};
	foreach my $pubID (sort keys %{$genes{$gene}->{publications}} ) {
		#print "$pubID\n";
		my $pub_item = $pubID{$pubID}->{'object'};
		push @pub_items,$pub_item;
	}
	$gene_item->set('publications', [@pub_items]);
}	

#write xml file
my $outfile = '/shared/data/pubmed/uploadFiles/ncbiID_pubmedID_no_anopheles.xml';
my $output = new IO::File(">$outfile");
my $writer = new XML::Writer(OUTPUT => $output, DATA_MODE => 1, DATA_INDENT => 3);
$writer->startTag('items');
for my $item (@items_to_write) {
  $item->as_xml($writer);
}
$writer->endTag('items');
$writer->end();
$output->close();

sub make_item
{
  my $implements = shift;
  my $item = $item_factory->make_item(implements => $implements);
  push @items_to_write, $item;
  return $item;
}
