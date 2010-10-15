#!/usr/bin/perl -w

#creates an xml file to link the ensembl geneid to it's translation Id for
#entering inparanoid data into flymine
#source files downloaded from BioMart for species that are sourced from ensembl.

use strict;
use warnings;

BEGIN {
    my $base = ( $0 =~ m:(.*)/.*: )[0];
    unshift(
        @INC,
        map( { $base . $_ } '/../../../intermine/perl/InterMine-Util/lib',
            '/../../../intermine/perl/InterMine-Item/lib',
        ),
    );
}

use InterMine::Item;
use InterMine::Item::Document;
use InterMine::Model;

my $model_file = '../../flymine/dbmodel/build/model/genomic_model.xml';
my $model = new InterMine::Model( file => $model_file );

my @files;
my $source = 'Ensembl';

#create the DataSource object

#identify the input files
my $source_dir = '/shared/data/orthologues/ensembl/geneid2translationid/';
my $outfile    = $source_dir . 'xml/geneId2translationId.xml';
my $doc        = InterMine::Item::Document->new(
    model  => $model,
    output => $outfile,
);
my $source_item = $doc->add_item( DataSource => ( name => $source, ), );
opendir( DIR, $source_dir ) || die("Cannot open directory !\n");
@files = grep( /txt$/, readdir DIR );
closedir(DIR);

#get the taxon ID from each file name and create the organism object
foreach my $file (@files) {
    my %genes;    #hash to store the gene objects
    my ($taxonID) = ( $file =~ /^(\d+)/ );

    my $org_item = $doc->add_item(
        Organism => (
            taxonId => $taxonID,
        ),
    );

    #add the path to the file name
    $file = $source_dir . $file;

    #open the file and read each line
    open( my $F, '<', $file ) or die "Cannot open $file, $!";
    while (<$F>) {

        my $gene_item;
        my @f = split /\t/;
        my ( $geneID, $translationID ) = ( $f[0], $f[1] );
        chomp $translationID;

        #print "$geneID $translationID\t";

#check that there is a translation ID and that the current line does not contain column titles
        if ( $translationID && $translationID ne 'Ensembl Peptide ID' ) {

        #create the translation object and add a reference to the synonym object
            my $trans_item = $doc->add_item(
                Translation => (
                    identifier => $translationID,
                    organism => $org_item,
                ),
            );

#if the gene has already been identified add a reference to it's object to the translation object
            if ( exists $genes{$geneID} ) {
                $gene_item = $genes{$geneID};
                $trans_item->set(gene => $gene_item );
                #print "Already found gene\n";
            }
            else {

#create the gene object, add a link to the gene synonym object and add the gene object to the hash
                $gene_item = $doc->add_item(
                    Gene => (
                        identifier => $geneID,
                        organism => $org_item,
                    ),
                );
                $genes{$geneID} = $gene_item;
                $trans_item->set( gene => $gene_item );

                #print "New gene\n";
            }
        }    #else{print"\n";}
    }

    close($F) or die "Cannot close $file, $!";
}    #end foreach
$doc->close();
