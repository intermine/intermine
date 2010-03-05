#!/usr/bin/perl
# rgd-qtls-to-xml.pl
# purpose: to create a target items xml file for intermine from RGD FTP file
# the script dumps the XML to STDOUT, as per the example on the InterMine wiki
# However, the script also creates a gff3 file to the location specified

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

my ($model_file, $qtls_file, $qtl_xml, $gff_file) = @ARGV;

unless ( $model_file ne '' and -e $model_file)
{
	print "\nrgd-qtls-to-xml.pl\n";
	print "Convert the QTLS_RAT flat file from RGD into InterMine XML\n";
	print "rgd-qtls-to-xml.pl model_file QTLS_RAT qtl_xml_output qtl_gff_output\n\n";
	exit(0);
}

my $data_source = 'RGD';
my $taxon_id = 10116;
my $output = new IO::File(">$qtl_xml");
my $writer = new XML::Writer(DATA_MODE => 1, DATA_INDENT => 3, OUTPUT => $output);

my %pubs = ();
my %genes =();
my @gff;



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

# read the genes file
open QTLS, $qtls_file;
my %index;
my $count = 0;
while(<QTLS>)
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
    	my @qtl_info = split(/\t/, $_);
		my $qtl_item = $item_factory->make_item('Qtl');
		$qtl_item->set('organism', $org_item);
		$qtl_item->set('primaryIdentifier', $qtl_info[$index{QTL_RGD_ID}]);
		$qtl_item->set('symbol', $qtl_info[$index{QTL_SYMBOL}]);
		
		my $syn_item = $item_factory->make_item('Synonym');
		$syn_item->set('value', $qtl_info[$index{QTL_SYMBOL}]);
		$syn_item->set('type', 'symbol');
		$syn_item->set('subject', $qtl_item);
		$syn_item->as_xml($writer);

		my $syn_item2 = $item_factory->make_item('Synonym');
		$syn_item2->set('value', $qtl_info[$index{QTL_NAME}]);
		$syn_item2->set('type', 'name');
		$syn_item2->set('subject', $qtl_item);
		$syn_item2->as_xml($writer);
				
		$qtl_item->set('lod', $qtl_info[$index{LOD}]) unless $qtl_info[$index{LOD}] eq '';
		$qtl_item->set('pValue', $qtl_info[$index{P_VALUE}]) unless $qtl_info[$index{P_VALUE}] eq '';
		$qtl_item->set('trait', $qtl_info[$index{TRAIT_NAME}]);
		$qtl_item->set('name', $qtl_info[$index{QTL_NAME}]);
		$qtl_item->set('synonyms', [$syn_item, $syn_item2]);
		
		unless($qtl_info[$index{'3.4_MAP_POS_START'}] eq '')
		{
			my @gff_line; #Create a GFF3 compatable line for each record
			push(@gff_line, $qtl_info[$index{CHROMOSOME_FROM_REF}]); #chromsome location
			push(@gff_line, "RatGenomeDatabase"); #source
			push(@gff_line, "Qtl"); #SO term
			push(@gff_line, $qtl_info[$index{'3.4_MAP_POS_START'}]); #start position
			push(@gff_line, $qtl_info[$index{'3.4_MAP_POS_STOP'}]); #stop position
			push(@gff_line, '.'); #score, left blank since QTLs have two different scores
			push(@gff_line, '.'); #strand, irrelevant
			push(@gff_line, '.'); #phase, irrelevant
			push(@gff_line, "ID=$qtl_info[$index{QTL_RGD_ID}]"); #attributes line
		
			push(@gff, join("\t", @gff_line)); #add line to gff list
		}
		
		
		#Add Publications
		if ($qtl_info[$index{CURATED_REF_PUBMED_ID}] ne '') {
	      	my @publication_info = split(/;/, $qtl_info[$index{CURATED_REF_PUBMED_ID}]);
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
	          		push(@currentPubs, $pub1);
	          		$pub1->as_xml($writer);
	        	}#end if-else
	      	}#end foreach
	      	$qtl_item->set("publications", \@currentPubs);
    	}#end if


		#Add Candidate Genes
		if($qtl_info[$index{CANDIDATE_GENE_RGD_IDS}])
		{
			my @gene_info = split(/;/, $qtl_info[$index{CANDIDATE_GENE_RGD_IDS}]);
			my @geneItems = ();
			foreach my $g (@gene_info)
			{
				if(exists $genes{$g})
				{ 
					my $qtls = $genes{$g}->get("parentQTLs");
					push(@$qtls, $qtl_item);
					$genes{$g}->set("parentQTLs", $qtls);
					push(@geneItems, $genes{$g});
				}
				else
				{
					my $gene_item = $item_factory->make_item("Gene");
					$gene_item->set('primaryIdentifier', $g);
					$gene_item->set('parentQTLs', [$qtl_item]);
					$gene_item->set('organism', $org_item);
					push(@geneItems, $gene_item);
					$genes{$g} = $gene_item;
				}#end if-else
			}#end foreach
			$qtl_item->set('candidateGenes', \@geneItems);
		}#end if
      	$qtl_item->as_xml($writer);
	} #end if-else	

}#end while
close QTLS;

#print out Genes
foreach my $g (keys %genes)
{
	$genes{$g}->as_xml($writer);
}

$writer->endTag("items");

open(GFF, ">$gff_file");
foreach my $line (@gff) {	print GFF "$line\n";	}
close GFF;
