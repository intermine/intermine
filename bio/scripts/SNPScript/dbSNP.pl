#!/usr/bin/perl

# translates SNP data from ensembl database to intermine items XML file

use strict;
use warnings;
use Switch;

BEGIN {
  # find the lib directory by looking at the path to this script
  # push (@INC, ($0 =~ m:(.*)/.*:)[0] .'../../../intermine/perl/lib/');
  # push (@INC, '/home/aw416/svn/dev/intermine/perl/lib');
  use lib '/home/aw416/svn/dev/intermine/perl/lib';
  print @INC;
  print "\n";
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
    die "usage: dbSNP.pl mine_name taxon_id data_destination \n eg. dbSNP.pl snpmine 9606 /data/ensembl \n";  
}

my ($mine_name, $taxon_ids, $data_destination) = @ARGV;

# config file to determine which chromosomes to parse
my %organisms = parse_orgs($taxon_ids);
my $config_file = '../../sources/ensembl/resources/ensembl_config.properties';
parse_config(read_file($config_file));

# properties file for database info
my $properties_file = "$ENV{HOME}/.intermine/$mine_name.properties";

# intermine
my $model_file = "../../../$mine_name/dbmodel/build/main/genomic_model.xml";
my $model = new InterMine::Model(file => $model_file);
my $item_factory = new InterMine::ItemFactory(model => $model);

# maps to prevent duplicate items
my %sourceMap;
my %statesMap;
my %typeMap;

my $start_time = time();
# TODO add date to file
my $outfile = $data_destination . '/ensembl.xml';
my $output = new IO::File(">$outfile");
my $writer = new XML::Writer(OUTPUT => $output, DATA_MODE => 1, DATA_INDENT => 3);
$writer->startTag('items');

my $datasource = 'Ensembl';
my $datasource_item = make_item("DataSource");
$datasource_item->set('name', $datasource);
$datasource_item->as_xml($writer);

my $org_item;
my $dataset_item;
my $count = 0;

# loop through each organism specified
foreach my $taxon_id(keys %organisms) {
    
    print "processing taxon_id $taxon_id\n";

    # databases
    #my $dbCore = get_db("core", $taxon_id); 
    #my $dbVariation = get_db("variation", $taxon_id); 

    # FIXME use get_db sub instead
    my $core_host = get_property_value("db.ensembl.$taxon_id.core.datasource.serverName", $properties_file);
    my $core_dbname = get_property_value("db.ensembl.$taxon_id.core.datasource.databaseName", $properties_file);
    my $core_user = get_property_value("db.ensembl.$taxon_id.core.datasource.user", $properties_file);
    my $core_pass = get_property_value("db.ensembl.$taxon_id.core.datasource.password", $properties_file);
    my $species = get_property_value("db.ensembl.$taxon_id.core.datasource.species", $properties_file);
    
    my $var_host = get_property_value("db.ensembl.$taxon_id.variation.datasource.serverName", $properties_file);
    my $var_dbname = get_property_value("db.ensembl.$taxon_id.variation.datasource.databaseName", $properties_file);
    my $var_user = get_property_value("db.ensembl.$taxon_id.variation.datasource.user", $properties_file);
    my $var_pass = get_property_value("db.ensembl.$taxon_id.variation.datasource.password", $properties_file);
    
    $org_item = make_item("Organism");
    $org_item->set("taxonId", $taxon_id);
    $org_item->as_xml($writer);

    $dataset_item = make_item("DataSet");
    $dataset_item->set('title', "$datasource data set for taxon id: $taxon_id");
    $dataset_item->as_xml($writer);

    my @chromosomes = ();
    my $chromosomes_string = $organisms{$taxon_id};

    if ($chromosomes_string) {
        @chromosomes = parse_chromosomes($chromosomes_string);
    } else {
        die "You must specify the chromosomes to process in the configuration file.\n";
    }

    # TODO query for list of chromosomes if not specified
    # loop through every chromosome
    while (my $chromosome = shift @chromosomes) {

        print "processing chromsome $chromosome\n";

        my $dbCore = Bio::EnsEMBL::DBSQL::DBAdaptor->new
            (-host => $var_host,
             -dbname => $core_dbname,
             -species => $species,
             -group => 'core',
             -user => $core_user,
             -pass => $core_pass);
    
        
        #get the database adaptor for Slice objects
        my $slice_adaptor = $dbCore->get_SliceAdaptor(); 
        my $slice = $slice_adaptor->fetch_by_region('chromosome',$chromosome);
        undef $slice_adaptor;
        
        #Connect to EnsEMBL variation database5C
        my $dbVariation = Bio::EnsEMBL::Variation::DBSQL::DBAdaptor->new
            (-host => $var_host,
             -dbname => $var_dbname,
             -species => $species,
             -group => 'variation',
             -user => $var_user,
             -pass => $var_pass);
    
        my $vf_adaptor = $dbVariation->get_VariationFeatureAdaptor(); 
        # get adaptor to VariationFeature object
        my $vfs = $vf_adaptor->fetch_all_by_Slice($slice); 
        # return ALL variations defined in $slice
        undef $vf_adaptor;
        
        # don't need to set the id, unless we use a shell script
        # my $chromosome_item = make_item_chromosome(id => $i);
        my $chromosome_item = make_item("Chromosome");
        $chromosome_item->set('primaryIdentifier', $slice->seq_region_name);
        $chromosome_item->as_xml($writer);

        # use while loop and shift instead of foreach to save memory
        while ( my $vf = shift @{$vfs} ) {
            #print "SNP NUMBER: ".$counter++." CHR:".$i."\n";
            my @alleles = split('[/.-]', $vf->allele_string);
            if(!$alleles[0]) {
                $alleles[0]='-';
            }
            if(!$alleles[1]) {
                $alleles[1]='-';
            }
            if(@alleles == 2) 
            {
                my $snp_item = make_item('EnsemblSNP');
                $snp_item->set('primaryIdentifier', $vf->variation_name);
                $snp_item->set('name', $vf->variation_name);
                $snp_item->set('allele1', $alleles[0]);
                $snp_item->set('allele2', $alleles[1]);
                $snp_item->set('chromosome', $chromosome_item);
      
				my $location_item = make_item("Location");
				$location_item->set("object", $chromosome_item);
				$location_item->set('start', $vf->start);
				$location_item->set('end', $vf->end);
				$location_item->set('subject', $snp_item);
				$location_item->as_xml($writer);
				$snp_item->set('chromosomeLocation', $location_item);
                

                my @stateItems = ();
                my $states = $vf->get_all_validation_states;
                while(my $state = shift(@$states)) 
                {
                    my $state_item;
                    if($statesMap{$state}) {
                        $state_item = $statesMap{$state};
                    } else {
                        $state_item = make_item('ValidationState');
                        $state_item->set('state', $state);
                        $statesMap{$state} = $state_item;
                        $state_item->as_xml($writer);
                    }#if-else
                    push(@stateItems, $state_item);
                }#foreach
                
                $snp_item->set('validations', [@stateItems]);
                
                my @typeItems = ();
                my $types = $vf->get_consequence_type;
                while(my $type = shift(@$types))
                {
                    my $type_item;
                    if($typeMap{$type}) {
                        $type_item = $typeMap{$type};
                    } else {
                        $type_item = make_item('ConsequenceType');
                        $type_item->set('type', $type);
                        $typeMap{$type} = $type_item;
                        $type_item->as_xml($writer);
                    }
                    push(@typeItems, $type_item);
                }#foreach    
                
                $snp_item->set('consequenceTypes', [@typeItems]);
                
                my @sourceItems = ();
                my $sources = $vf->get_all_sources;
                while(my $source = shift @$sources)
                {
                    my $source_item;
                    if ($sourceMap{$source}) {
                        $source_item = $sourceMap{$source};
                    } else {
                        $source_item = make_item('Source');
                        $source_item->set('source',$source);
                        $sourceMap{$source} = $source_item;
                        $source_item->as_xml($writer);
                    }
                    push(@sourceItems, $source_item);
                }#foreach
                $snp_item->set('sources', [@sourceItems]);
                $snp_item->as_xml($writer);
                $location_item = $location_item->destroy;
                $snp_item = $snp_item->destroy;
            }#if
       
            undef $dbCore;
            undef $dbVariation;
        }
        
    }
    my $end_time = time();
    my $action_time = $end_time - $start_time;
    
    #write xml file
    $writer->endTag('items');
    $writer->end();
    $output->close();
    
    $end_time = time();
    $action_time = $end_time - $start_time;
    print "creating the XML file took $action_time seconds and created $count items\n";
    
}

sub make_item{
    my $implements = shift;
    my $item = $item_factory->make_item(implements => $implements);
    #push @items, $item;    
    if ($item->valid_field('organism')) {
        $item->set('organism', $org_item);
    }
    if ($item->valid_field('dataSets') && $implements ne 'DataSource') {
        $item->set('dataSets', [$dataset_item]);
    }
    if ($item->valid_field('dataSource')) {
        $item->set('dataSource', $datasource_item);
    }
    $count++; # just for reporting purposes
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

# get db info
sub get_db {

    my($group, $taxon_id) = @_;

    my $host = get_property_value("db.ensembl.$taxon_id.$group.datasource.serverName", $properties_file);
    my $dbname = get_property_value("db.ensembl.$taxon_id.$group.datasource.databaseName", $properties_file);
    my $user = get_property_value("db.ensembl.$taxon_id.$group.datasource.user", $properties_file);
    my $pass = get_property_value("db.ensembl.$taxon_id.$group.datasource.password", $properties_file);
    my $species = get_property_value("db.ensembl.$taxon_id.$group.datasource.species", $properties_file);
    
    my $db = Bio::EnsEMBL::DBSQL::DBAdaptor->new
        (-host => $host,
         -dbname => $dbname,
         -species => $species,
         -group => $group,
         -user => $user,
         -pass => $pass);
    return $db;
}

# parse the config file
sub parse_config {
    my (@lines) = @_;
    foreach (@lines) {
        my $line = $_;
        my ($taxon_id, $config) = split("\\.", $line);
        my ($label, $value) = split("\\=", $config);
        if ($label eq 'chromosomes' && defined $organisms{$taxon_id}) {
            $organisms{$taxon_id} = $value;
        }
    }
    return;
}

# user can specify which chromosomes to load  
# eg 1-21,X,Y
sub parse_chromosomes {
    my ($chromosome_string) = shift;
  
    my @bits = split(",", $chromosome_string);
    my @chromosomes = ();
 
    foreach (@bits) {
        my $bit = $_;
        
        # list may be a range
        if ($bit =~ "-") {
            my @range = split("-", $bit);
            my $min = $range[0];
            my $max = $range[1];
            for (my $i = $min; $i <= $max; $i++) {
                push(@chromosomes, $i);
            }
        } else {
            push(@chromosomes, $bit);
        }
    }
    return @chromosomes;
}

# TODO I don't think this is used
sub make_synonym {
  my ($subject, $type, $value) = @_;
  my $key = $subject . $type . $value;

  my $syn = make_item("Synonym");
  $syn->set('subject', $subject);
  $syn->set('type', $type);
  $syn->set('value', $value);
  $syn->set('isPrimary', 'true');
}

sub parse_orgs {
    my ($taxon_ids) = @_;
    my %orgs = ();    
    for (split("\\,", $taxon_ids)) {
        $orgs{$_} = "";
    }
    return %orgs;
}


exit 0;

