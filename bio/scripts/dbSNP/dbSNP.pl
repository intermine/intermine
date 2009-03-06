#!/usr/bin/perl

# translates SNP data from ensembl database to intermine items XML file

use strict;
use warnings;
use Switch;

BEGIN {
  # find the lib directory by looking at the path to this script
    push (@INC, ($0 =~ m:(.*)/.*:)[0] .'/../../../intermine/perl/lib/');
}

use XML::Writer;
use InterMine::Item;
use InterMine::ItemFactory;
use InterMine::Model;
use InterMine::Util qw(get_property_value);
use IO qw(Handle File);
use Cwd;

use Bio::EnsEMBL::Variation::DBSQL::DBAdaptor;
use Bio::EnsEMBL::DBSQL::DBAdaptor;

if (@ARGV != 3) {
    die "usage: mine_name taxonId data_destination \n eg. flymine 7227 /data/ensembl \n";  
}

my ($mine_name, $taxon_id, $data_destination) = @ARGV;

# FIXME
#my $config_file = '../../sources/ensembl/resources/ensembl_config.properties';
#parse_config(read_file($config_file));

my $properties_file = "$ENV{HOME}/.intermine/$mine_name.properties";

my $host = get_property_value("db.ensembl.$taxon_id.core.datasource.serverName", $properties_file);
my $dbname = get_property_value("db.ensembl.$taxon_id.core.datasource.databaseName", $properties_file);
my $user = get_property_value("db.ensembl.$taxon_id.core.datasource.user", $properties_file);
my $pass = get_property_value("db.ensembl.$taxon_id.core.datasource.password", $properties_file);
my $species = get_property_value("db.ensembl.$taxon_id.core.datasource.species", $properties_file);

my $dbCore = Bio::EnsEMBL::DBSQL::DBAdaptor->new
    (-host => $host,
     -dbname => $dbname,
     -species => $species,
     -group => 'core',
     -user => $user,
     -pass => $pass);

$host = get_property_value("db.ensembl.$taxon_id.variation.datasource.serverName", $properties_file);
$dbname = get_property_value("db.ensembl.$taxon_id.variation.datasource.databaseName", $properties_file);
$user = get_property_value("db.ensembl.$taxon_id.variation.datasource.user", $properties_file);
$pass = get_property_value("db.ensembl.$taxon_id.variation.datasource.password", $properties_file);


#Connect to EnsEMBL variation database
my $dbVariation = Bio::EnsEMBL::Variation::DBSQL::DBAdaptor->new
   (-host => $host,
     -dbname => $dbname,
     -species => $species,
     -group => 'variation',
     -user => $user,
    -pass => $pass);

my @items = ();

my $model_file = "../../../$mine_name/dbmodel/build/main/genomic_model.xml";
my $model = new InterMine::Model(file => $model_file);
my $item_factory = new InterMine::ItemFactory(model => $model);

my %sourceMap;
my %statesMap;
my %typeMap;

for (my $i=1; $i<=24; $i++) { 
    my @items_to_write = ();
    my @files;
    my $slice_adaptor = $dbCore->get_SliceAdaptor(); #get the database adaptor for Slice objects
    my $slice;
    switch($i) {
        case [1..22] {
            $slice = $slice_adaptor->fetch_by_region('chromosome',$i);                  
        } case 23 {
            $slice = $slice_adaptor->fetch_by_region('chromosome','X');                  
        } case 24 {
            $slice = $slice_adaptor->fetch_by_region('chromosome','Y');                 
        }
    }
    my $vf_adaptor = $dbVariation->get_VariationFeatureAdaptor(); 
    #get adaptor to VariationFeature object
    my $vfs = $vf_adaptor->fetch_all_by_Slice($slice); 
    #return ALL variations defined in $slice
    my $counter = 1;
    
    my $chromosome_item = make_item_chromosome(id => $i);
    $chromosome_item->set('primaryIdentifier', $slice->seq_region_name);
    
    foreach my $vf (@{$vfs}){
        print "SNP NUMBER: ".$counter++." CHR:".$i."\n";
        my @alleles = split('[/.-]', $vf->allele_string);
        if(!$alleles[0]) {
            $alleles[0]='-';
        }
        if(!$alleles[1]) {
            $alleles[1]='-';
        }
        if(@alleles == 2) {
            my $snp_item = make_item('EnsemblSNP');
            $snp_item->set('snp', $vf->variation_name);
            $snp_item->set('allele1', $alleles[0]);
            $snp_item->set('allele2', $alleles[1]);
            $snp_item->set('chromosomeStart', $vf->start);
            $snp_item->set('chromosomeEnd', $vf->end);
            
            my @stateItems;
            foreach my $state (@{$vf->get_all_validation_states}) {
                my $state_item;
                if($statesMap{$state}) {
                    $state_item = $statesMap{$state};
                } else {
                    $state_item = make_item('ValidationState');
                    $state_item->set('state', $state);
                    $statesMap{$state} = $state_item;
                }
                push(@stateItems, $state_item);
            }	
            
            $snp_item->set('validations', [@stateItems]);
            
            my @typeItems;
            foreach my $type (@{$vf->get_consequence_type}) {
                my $type_item;
                if($typeMap{$type}) {
                    $type_item = $typeMap{$type};
                } else {
                    $type_item = make_item('ConsequenceType');
                    $type_item->set('type', $type);
                    $typeMap{$type} = $type_item;
                }
                push(@typeItems, $type_item);
            }
            
            $snp_item->set('consequenceTypes', [@typeItems]);
            
            my @sourceItems;
            foreach my $source (@{$vf->get_all_sources}) {
                my $source_item;
                if ($sourceMap{$source}) {
                    $source_item = $sourceMap{$source};
                } else {
                    $source_item = make_item('Source');
                    $source_item->set('source',$source);
                    $sourceMap{$source} = $source_item;
                }
                push(@sourceItems, $source_item);
            }
            $snp_item->set('chromosome', $chromosome_item);
            $snp_item->set('sources', [@sourceItems]);
        }
    }

    #write xml filea
    my $outfile =$data_destination . 'Chromosome'.$i.'.xml';
    my $output = new IO::File(">$outfile");
    my $writer = new XML::Writer(OUTPUT => $output, DATA_MODE => 1, DATA_INDENT => 3);
    $writer->startTag('items');
    for my $item (@items) {
        $item->as_xml($writer);
    }
    $writer->endTag('items');
    $writer->end();
    $output->close();
}
sub make_item{
    my $implements = shift;
    my $item = $item_factory->make_item(implements => $implements);
    push @items, $item;
    return $item;
}

sub make_item_chromosome{
    my %opts = @_;
    my $item = $item_factory->make_item(implements => 'Chromosome');
    $item->setPref(refId => $opts{id});
    push @items, $item;
    return $item;
}

# read in the config file
sub read_file {
    my($filename) = shift;
    my @lines = ();
    open(FILE, "< $filename") or die "Can't open $filename : $!";
    while(<FILE>) {
        s/#.*//;            # ignore comments by erasing them
        next if /^(\s)*$/;  # skip blank lines
        chomp;              # remove trailing newline characters
        push @lines, $_;    # push the data line onto the array
    }
    close FILE;
    return @lines;  
}


exit 0;
