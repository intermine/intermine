#!/usr/bin/perl -w

#creates xml files (one for each species) to link the ensembl geneid to it's translation Id for 
#entering inparanoid data into flymine
#source files downloaded from BioMart for species that are sourced from ensembl. 

use strict;
use warnings;

BEGIN {
    my $base = ( $0 =~ m:(.*)/.*: )[0];
    unshift( @INC, 
        map( {$base . $_} 
            '/../../../intermine/perl/InterMine-Util/lib',
            '/../../../intermine/perl/InterMine-Item/lib',
        ),
    );
}

use InterMine::Item;
use InterMine::Item::Document;
use InterMine::Model;

#Kim's stuff for writing XML
my @items = ();
my $model_file = '../../flymine/dbmodel/build/model/genomic_model.xml';
my $model = InterMine::Model->new(file => $model_file);

my @files;
my $source = 'Ensembl';

#identify the input files
my $source_dir ='/shared/data/orthologues/ensembl/geneid2translationid/';
opendir(DIR,$source_dir) || die("Cannot open directory !\n");
@files = grep(/txt$/, readdir DIR);
closedir(DIR);

#get the taxon ID from each file name and create the organism object
foreach my $file (@files){
    my $outfile = $source_dir.'xml/'.$taxonID.'_geneId2translationId.xml';
    $doc = InterMine::Item::Document->new(
        model => $model, 
        output => $outfile,
    );

	my %genes; #hash to store the gene objects
	my ($taxonID) = ($file=~/^(\d+)/);
	
	#create the DataSource object
	my $source_item = $doc->add_item('DataSource');
	$source_item->set('name', $source);
	
	my $org_item = $doc->add_item('Organism');
	$org_item->set('taxonId', $taxonID);
	
	#print "file $file Taxon $taxonID\n";
	
	#add the path to the file name
	$file = $source_dir . $file;
	
	#open the file and read each line
	open(my $F, '<', $file) or die "Could not open $file, $!";
	while(<$F>){
		
		my $gene_item;
		my @f = split/\t/;
		my ($geneID,$translationID) = ($f[0],$f[1]);
		 
		chomp $translationID;
		#print "$geneID $translationID\t";
		
		#check that there is a translation ID and that the current line does not contain column titles
		if($translationID && $translationID ne 'Ensembl Peptide ID'){
			
			#create the translation object and add a reference to the synonym object
			my $trans_item = $doc->add_item(
                Translation => (
                    identifier => $translationID,
                    organism   => $org_item,
                ),
            );
			
			#if the gene has already been made, add a reference to the translation object
			if(exists $genes{$geneID}){
				$gene_item = $genes{$geneID};
				$trans_item->set(gene => $gene_item);
				#print "Already found gene\n";
			}else{
				#create the gene object, add a link to the gene synonym object and add the gene object to the hash
				$gene_item = $doc->add_item(
                    Gene => (
                        identifier => $geneID,
                        organism => $org_item,
                    ),
                );
                $genes{$geneID} = $gene_item;
				$trans_item->set(gene => $gene_item);
				#print "New gene\n";
			}
		}#else{print"\n";}
	}	

	close($F) or die "Could not close $file, $!";
    $doc->close;
}
