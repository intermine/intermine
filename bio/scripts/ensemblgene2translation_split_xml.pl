#!/usr/bin/perl -w

#creates xml files (one for each species) to link the ensembl geneid to it's translation Id for 
#entering inparanoid data into flymine
#source files downloaded from BioMart for species that are sourced from ensembl. 

use strict;
use warnings;

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

my @files;
my $source = 'Ensembl';



#identify the input files
my $source_dir ='/shared/data/orthologues/ensembl/geneid2translationid/';
opendir(DIR,$source_dir) || die("Cannot open directory !\n");
@files=grep(/txt$/,readdir DIR);
closedir(DIR);

#get the taxon ID from each file name and create the organism object
foreach my $file (@files){
	my %genes; #hash to store the gene objects
	my ($taxonID) = ($file=~/^(\d+)/);
	
	#create the DataSource object
	my $source_item = make_item('DataSource');
	$source_item->set('name', $source);
	
	my $org_item = make_item('Organism');
	$org_item->set('taxonId', $taxonID);
	
	#print "file $file Taxon $taxonID\n";
	
	#add the path to the file name
	$file =$source_dir.$file;
	
	#open the file and read each line
	open(F,"<$file") or die "$!";
	while(<F>){
		
		my $gene_item;
		my @f = split/\t/;
		my ($geneID,$translationID) = ($f[0],$f[1]);
		 
		chomp $translationID;
		#print "$geneID $translationID\t";
		
		#check that there is a translation ID and that the current line does not contain column titles
		if($translationID && $translationID ne 'Ensembl Peptide ID'){
			
			#create the synonym object and add a reference to the source object
			my $t_syn_item = make_item('Synonym');
			$t_syn_item->set('value', $translationID);
			$t_syn_item->set('type', 'identifier');
			$t_syn_item->set('source', $source_item);
			
			#create the translation object and add a reference to the synonym object
			my $trans_item = make_item('Translation');
			$trans_item->set('identifier', $translationID);
			$trans_item->set('synonyms', [$t_syn_item]);
			$trans_item->set('organism', $org_item);
			
			#if the gene has already been identified add a reference to it's object to the translation object
			if(exists $genes{$geneID}){
				$gene_item = $genes{$geneID}->{'object'};
				$trans_item->set('gene', $gene_item);
				#print "Already found gene\n";
			}else{
				#else create a new gene synonym object. add reference to the source object
				my $g_syn_item = make_item('Synonym');
				$g_syn_item->set('value', $geneID);
				$g_syn_item->set('type', 'identifier');
				$g_syn_item->set('source', $source_item);
				
				#create the gene object, add a link to the gene synonym object and add the gene object to the hash
				$gene_item = make_item('Gene');
				$gene_item->set('identifier', $geneID);
				$gene_item->set('synonyms', [$g_syn_item]);
				$gene_item->set('organism', $org_item);
				$genes{$geneID}={'object' => $gene_item};
				$trans_item->set('gene', $gene_item);
				#print "New gene\n";
			}
		}#else{print"\n";}
	}	

	close(F) or die "$!";
	

#write xml file
my $outfile =$source_dir.'xml/'.$taxonID.'_geneId2translationId.xml';
my $output = new IO::File(">$outfile");
my $writer = new XML::Writer(OUTPUT => $output, DATA_MODE => 1, DATA_INDENT => 3);
$writer->startTag('items');
for my $item (@items_to_write) {
  $item->as_xml($writer);
}
$writer->endTag('items');
$writer->end();
$output->close();
@items_to_write = ();
}#end foreach
#kim's subroutine. Organsim object references are automatically created where necessary
	sub make_item{
  		my $implements = shift;
  		my $item = $item_factory->make_item(implements => $implements);
  		push @items_to_write, $item;
  		return $item;
	}
