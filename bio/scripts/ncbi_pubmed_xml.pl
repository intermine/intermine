#!/usr/bin/perl -w

#usage: takes data from /shared/data/ncbigene/gene2pubmed as $ARGV[0] and writes the xml file
#for linking ncbi gene ID to one or more pubmed IDs. 
#Final xml file should be copied to /shared/data/pubmed/uploadFiles/

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
my $model_file = '../svn/dev/flymine/dbmodel/build/model/genomic_model.xml';
my $model = new InterMine::Model(file => $model_file);
my $item_factory = new InterMine::ItemFactory(model => $model);
my @items_to_write = ();


#define organism ids to include
my %ids=("4932",'S.cerevisiae',
	 "6239",'C.elegans',
	 "7227",'D.melanogaster',
	 "180454",'A.gambiae str PEST');
my (%genes,%pubID);

#open /shared/data/ncbigene/gene2pubmed and get data
open(F,"<$ARGV[0]") or die "$!";
while(<F>){
	my @f = split/\t/;
	my $current_ID=$f[0];
	#only use the organisms in %ids
	if(exists $ids{$current_ID}){
		chomp $f[2];
		
		$genes{$f[1]} = $f[1];
		$pubID{$f[2].$f[1]} = {
		'gene' => $f[1],
		'pubmed' => $f[2]
		};
	}
}	
close(F) or die "$!";

#write ncbiID/pubmedID pairs to a file to speed the next bit up
my $filepath = "./pairs.txt"; 
my $ID;
open(RESULT, ">$filepath") || die "$!";
foreach $ID (sort keys %pubID){
  print RESULT "$pubID{$ID}->{'gene'}\t$pubID{$ID}->{'pubmed'}\n";
}
close(RESULT);

#for each gene, find which pubmedIds reference it
foreach my $geneID (sort keys %genes){
	my @pub_items;
	print "Gene $geneID has publications:\n";
	my $gene1_item = make_item('Gene');
	$gene1_item->set('ncbiGeneNumber', $geneID);
	
	open(F,"<$filepath") or die "$!";
	while(<F>){
		my @f = split/\t/;
		my $current_ID=$f[0];
		if($current_ID==$geneID){
			chomp $f[1];
			print "$f[1]\n";
			my $pub1_item = make_item('Publication');
			my $pubmedID = $f[1];
			$pub1_item->set('pubMedId', $pubmedID);
			push @pub_items,$pub1_item;
	}

	}close(F) or die "$!";
	$gene1_item->set('evidence', [@pub_items]);
}	
#delete ncbiID/pubmedID pairs file
unlink ("$filepath")|| die "Cannot rm $filepath: $!";

#write xml file
my $output1 = new IO::File(">ncbiID_pubmedID.xml");
my $writer1 = new XML::Writer(OUTPUT => $output1, NEWLINES => 1);

my $writer = new XML::Writer(OUTPUT => $output1, DATA_MODE => 1, DATA_INDENT => 3);
$writer->startTag('items');
for my $item (@items_to_write) {
  $item->as_xml($writer);
}
$writer->endTag('items');
$writer->end();
$output1->close();

sub make_item
{
  my $implements = shift;
  my $item = $item_factory->make_item(implements => $implements);
  push @items_to_write, $item;
  return $item;
}
