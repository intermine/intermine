#!/usr/bin/perl

# translates SNP data from ensembl database to intermine items XML file

use strict;
use warnings;
use Switch;

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
use InterMine::Util qw(get_property_value);
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
chomp( my $date = qx(date "+%F") );
my $outfile = "$data_destination/ensembl-$date.xml";

# intermine
my $model_file = "../../../$mine_name/dbmodel/build/main/genomic_model.xml";
my $model = new InterMine::Model(file => $model_file);
my $doc = new InterMine::Item::Document(
    model => $model,
    output => $outfile,
);

# maps to prevent duplicate items
my %sourceMap;
my %statesMap;
my %typeMap;

my $start_time = time();

my $datasource = 'Ensembl';
my $datasource_item = make_item("DataSource");
$datasource_item->set(name => $datasource);
$doc->write($datasource_item);

my $org_item;
my $dataset_item;
my $count = 0;

# loop through each organism specified
while (my ($taxon_id, $chromosomes) = each %organisms) {
    
    print "processing taxon_id $taxon_id\n";

    $org_item = make_item("Organism");
    $org_item->set("taxonId", $taxon_id);

    $dataset_item = make_item("DataSet");
    $dataset_item->set(name => "$datasource data set for taxon id: $taxon_id");

    $doc->write($org_item, $dataset_item);

    my @chromosomes = parse_chromosomes($chromosomes);

    # TODO query for list of chromosomes if not specified
    # loop through every chromosome
    while (my $chromosome = shift @chromosomes) {

        print "processing chromsome $chromosome\n";
        
        my ($dbCore, $dbVariation) = get_db($taxon_id);
        
        #get the database adaptor for Slice objects
        my $slice_adaptor = $dbCore->get_SliceAdaptor(); 
        my $slice = $slice_adaptor->fetch_by_region('chromosome',$chromosome);
    
        my $vf_adaptor = $dbVariation->get_VariationFeatureAdaptor(); 
        # get adaptor to VariationFeature object
        my $vfs = $vf_adaptor->fetch_all_by_Slice($slice); 
        # return ALL variations defined in $slice
        
        my $chromosome_item = make_item("Chromosome");
        $chromosome_item->set('primaryIdentifier', $slice->seq_region_name);
        $doc->write($chromosome_item);

        # use while loop and shift instead of foreach to save memory
        while ( my $vf = shift @{$vfs} ) {
            #print "SNP NUMBER: ".$counter++." CHR:".$i."\n";
            my @alleles = split(/[\/.-]/, $vf->allele_string);

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
                
                my @stateItems = ();
                foreach my $state (@{$vf->get_all_validation_states}) {
                    my $state_item;
                    if($statesMap{$state}) {
                        $state_item = $statesMap{$state};
                    } else {
                        $state_item = make_item('ValidationState');
                        $state_item->set('state', $state);
                        $statesMap{$state} = $state_item;
                        $doc->write($state_item);
                    }
                    push(@stateItems, $state_item);
                }	
                
                $snp_item->set('validations', [@stateItems]);
                
                my @typeItems = ();
                foreach my $type (@{$vf->get_consequence_type}) {
                    my $type_item;
                    if($typeMap{$type}) {
                        $type_item = $typeMap{$type};
                    } else {
                        $type_item = make_item('ConsequenceType');
                        $type_item->set('type', $type);
                        $typeMap{$type} = $type_item;
                        $doc->write($type_item);
                    }
                    push(@typeItems, $type_item);
                }
                
                $snp_item->set('consequenceTypes', [@typeItems]);
                
                my @sourceItems = ();
                foreach my $source (@{$vf->get_all_sources}) {
                    my $source_item;
                    if ($sourceMap{$source}) {
                        $source_item = $sourceMap{$source};
                    } else {
                        $source_item = make_item('Source');
                        $source_item->set('source',$source);
                        $sourceMap{$source} = $source_item;
                        $doc->write($source_item);
                    }
                    push(@sourceItems, $source_item);
                }
                $snp_item->set('chromosome', $chromosome_item);
                $snp_item->set('sources', [@sourceItems]);
                $doc->write($snp_item);
            }
       
            undef $dbCore;
            undef $dbVariation;
        }
        
    }
    my $end_time = time();
    my $action_time = $end_time - $start_time;
    
    #write xml file
    $doc->close();
    
    $end_time = time();
    $action_time = $end_time - $start_time;
    print "creating the XML file took $action_time seconds and created $count items\n";
    
}

sub get_db_props {
    my $taxon_id = shift;
    my %db_props;
    # core host is not used
    $db_props{core_host} = get_property_value("db.ensembl.$taxon_id.core.datasource.serverName", $properties_file);
    $db_props{core_dbname} = get_property_value("db.ensembl.$taxon_id.core.datasource.databaseName", $properties_file);
    $db_props{core_user} = get_property_value("db.ensembl.$taxon_id.core.datasource.user", $properties_file);
    $db_props{core_pass} = get_property_value("db.ensembl.$taxon_id.core.datasource.password", $properties_file);
    $db_props{species} = get_property_value("db.ensembl.$taxon_id.core.datasource.species", $properties_file);
    
    $db_props{var_host} = get_property_value("db.ensembl.$taxon_id.variation.datasource.serverName", $properties_file);
    $db_props{var_dbname} = get_property_value("db.ensembl.$taxon_id.variation.datasource.databaseName", $properties_file);
    $db_props{var_user} = get_property_value("db.ensembl.$taxon_id.variation.datasource.user", $properties_file);
    $db_props{var_pass} = get_property_value("db.ensembl.$taxon_id.variation.datasource.password", $properties_file);
    return %db_props;
}
sub make_item{
    my $implements = shift;
    my $item = $doc->make_item(implements => $implements);
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
    my $taxon_id = shift;
    my %props = get_db_props($taxon_id);

    my $dbCore = Bio::EnsEMBL::DBSQL::DBAdaptor->new
        (-host => $props{var_host},
        -dbname => $props{core_dbname},
        -species => $props{species},
        -group => 'core',
        -user => $props{core_user},
        -pass => $props{core_pass});
    my $dbVariation = Bio::EnsEMBL::Variation::DBSQL::DBAdaptor->new
        (-host => $props{var_host},
        -dbname => $props{var_dbname},
        -species => $props{species},
        -group => 'variation',
        -user => $props{var_user},
        -pass => $props{var_pass});
    return $dbCore, $dbVariation;;
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
    my $chromosome_string = shift;
    die "You must specify the chromosomes to process in the configuration file.\n"
        unless $chromosome_string;
  
    my @bits = split(/,/, $chromosome_string);
    my @chromosomes = ();
 
    foreach (@bits) { # Handles single values and ranges x-y
        if (/-/) {
            my ($min, $max, @rest) = split /-/;
            if (@rest or not $min <= $max) {
                die "Bad range: $_";
            }
            push @chromosomes, $min .. $max;
        } else {
            push @chromosomes, $_;
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
    my %orgs;    
    for (split(/,/, $taxon_ids)) {
        $orgs{$_} = undef;
    }
    return %orgs;
}


exit 0;
