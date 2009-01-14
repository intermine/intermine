#!/usr/bin/perl

# translates data from ensembl database to intermine items XML file

BEGIN {
    push (@INC, ($0 =~ m:(.*)/.*:)[0] . '/../../intermine/perl/lib');    
}

use strict;
use warnings;
use Switch;

use XML::Writer;
use InterMine::Item;
use InterMine::ItemFactory;
use InterMine::Model;
use Bio::EnsEMBL::DBSQL::DBAdaptor;
use InterMine::Util qw(get_property_value);
use IO qw(Handle File);
use Cwd;
use Digest::MD5 qw(md5);

if (@ARGV != 3) {
  die "usage: mine_name taxon_ids data_destination \n eg. flymine 9606 /shared/data/ensembl/current\n";  
}

my ($mine_name, $taxon_ids, $data_destination) = @ARGV;

my $start_time = time();

my $release;
my $model_file = "../../$mine_name/dbmodel/build/main/genomic_model.xml";
my $model = new InterMine::Model(file => $model_file);
my $item_factory = new InterMine::ItemFactory(model => $model);

my $datasource = 'Ensembl';
my @items = (); 
my %organisms = parse_orgs($taxon_ids);
my $datasource_item = make_item("DataSource");
$datasource_item->set('name', $datasource);
my $org_item;
my $dataset_item;

my $config_file = '../sources/ensembl/resources/ensembl.config';
parse_config(read_file($config_file));

my $properties_file = "$ENV{HOME}/.intermine/$mine_name.properties";
if ($release) {
  $properties_file .= ".$release";
}

foreach my $taxon_id(keys %organisms) {
    
    my %proteins = ();
    my %exons = ();

    $org_item = make_item("Organism");
    $org_item->set("taxonId", $taxon_id);

    $dataset_item = make_item("DataSet");
    $dataset_item->set('title', "$datasource data set for taxon id: $taxon_id");

    my %chromosomes = ();
    my $chromosomes_string = $organisms{$taxon_id};

    if ($chromosomes_string) {
        %chromosomes = parse_chromosomes($chromosomes_string);
    }

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
    
    my $slice_adaptor = $dbCore->get_sliceAdaptor();
    my @slices = @{$slice_adaptor->fetch_all('toplevel')};

    while (my $slice = shift @slices) {
        my $chromosome_name = $slice->seq_region_name();
        if ($chromosomes_string && !exists $chromosomes{$chromosome_name}) {
            # skip this gene. it's not on a chromosome of interest
            next;
        }
        my $chromosome_item = make_chromosome(\%chromosomes, $chromosome_name);

        my @genes =  @{$slice->get_all_Genes(undef,undef,1)};

        while (my $gene = shift @genes) {

            my $gene_item = make_item("Gene");
            my $gene_type = $gene->biotype();
            
            $gene_item->set('featureType', $gene_type);
            
            my $curated = 'false';
            if ($gene->status eq 'KNOWN') {
                $curated = 'true';
            }
            $gene_item->set('curated', $curated);
            
            parseFeature($gene, $gene_item, $chromosome_item);

            my @transcripts = @{ $gene->get_all_Transcripts() };
            while ( my $transcript = shift @transcripts ) {
                
                my $transcript_item;
                if ($gene_type eq "protein_coding") {
                    $transcript_item = make_item("MRNA");
                } else {
                    $transcript_item = make_item("Transcript");
                }
                $transcript_item->set('gene', $gene_item);
                $transcript_item->set('sequence', make_seq($transcript->seq->seq));
                parseFeature($transcript, $transcript_item, $chromosome_item);
          
                if ($gene_type eq "protein_coding") {

                    my $translation = $transcript->translate();
                    if (!defined $translation) {
                        die "bad translation\n";
                    }
                    my $protein_seq = $translation->seq();
                    my $protein_item = make_protein(\%proteins, $protein_seq);
                
                    $protein_item->set('genes', [$gene_item]);
                    $protein_item->set('transcripts', [$transcript_item]);

                    my $ctx = Digest::MD5->new;
                    $ctx->add($protein_seq);
                    my $digest = $ctx->hexdigest;
                    
                    $protein_item->set('md5checksum', $digest);

                    my $cds_item = make_item("CDS");
                    my $cds_primaryIdentifier = $transcript->stable_id() . "_CDS";
                    my $cds_seq = make_seq($transcript->translateable_seq());

                    $cds_item->set('primaryIdentifier',$cds_primaryIdentifier);                    
                    $cds_item->set('sequence', $cds_seq);
                    $cds_item->set('MRNA', $transcript_item);
                    $cds_item->set('protein', $protein_item);
                }
                                
                my @exons = @{ $transcript->get_all_Exons() };
                while ( my $exon = shift @exons ) {
                    my $exon_item = make_exon(\%exons, $exon->stable_id());
                    $exon_item->set('transcripts', [$transcript_item]);
                    $exon_item->set('gene', $gene_item);                    
                    $exon_item->set('sequence', make_seq($exon->seq->seq));
                    parseFeature($exon, $exon_item, $chromosome_item);                   
                }
            }
        }    
    }
    my $end_time = time();
    my $action_time = $end_time - $start_time;
    print "processing the files for $taxon_id took $action_time seconds.  now creating the XML file... \n";
    
#write xml file
    $start_time = time();
    my $output = new IO::File(">$data_destination/$taxon_id.xml");
    my $writer = new XML::Writer(DATA_MODE => 1, DATA_INDENT => 3, OUTPUT => $output);
    $writer->startTag('items');
    for my $item (@items) {
        $item->as_xml($writer);
    }
    $writer->endTag('items');
    $writer->end();
    $end_time = time();
    $action_time = $end_time - $start_time;
    print "creating the XML file for $taxon_id took $action_time seconds.\n";
}

# helper method that makes a new object of a particular class and saves it in 
# the @items array
sub make_item {
    my $implements = shift;
    my $item = $item_factory->make_item(implements => $implements);
    push @items, $item;
    if ($item->valid_field('organism')) {
        $item->set('organism', $org_item);
    }
    if ($item->valid_field('dataSets') && $implements ne 'DataSource') {
        $item->set('dataSets', [$dataset_item]);
    }
    if ($item->valid_field('dataSource')) {
        $item->set('dataSource', $datasource_item);
    }
    return $item;
}

# parses the feature returned from ensembl and 
# assigns the classes to the intermine item
# used for genes, transcripts, and exons.  assumes has seq(0
sub parseFeature {

    my ($feature, $item, $chromosome) = @_;

    my $location = make_item("Location");
    $location->set('start', $feature->start());
    $location->set('end', $feature->end());
    $location->set('strand', $feature->strand());
    $location->set('subject', $item);
    $location->set('object', $chromosome);

    $item->set('primaryIdentifier', $feature->stable_id());
    $item->set('chromosomeLocation', $location);
    $item->set('chromosome', $chromosome);
    
    make_synonym($item, "identifier", $feature->stable_id());

    return;
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
    my %chromosomes = ();
 
    foreach (@bits) {
        my $bit = $_;
        
        # list may be a range
        if ($bit =~ "-") {
            my @range = split("-", $bit);
            my $min = $range[0];
            my $max = $range[1];
            for (my $i = $min; $i <= $max; $i++) {
                $chromosomes{$i} = undef;
            }
        } else {
            $chromosomes{$bit} = undef;
        }
    }
    return %chromosomes;
}

sub make_synonym {
  my ($subject, $type, $value) = @_;

  my $syn = make_item("Synonym");
  $syn->set('subject', $subject);
  $syn->set('type', $type);
  $syn->set('value', $value);
  $syn->set('source', $datasource_item);
  $syn->set('isPrimary', 'true');
}

sub make_chromosome {

    my ($chromosomes, $chromosome_name) = @_;
    my $chromosome_item;
       
    if (defined $chromosomes->{$chromosome_name}) {
        $chromosome_item = $chromosomes->{$chromosome_name};
    } else {
        $chromosome_item = make_item("Chromosome");
        $chromosome_item->set('primaryIdentifier', $chromosome_name);
        $chromosomes->{$chromosome_name} = $chromosome_item;
        make_synonym($chromosome_item, "identifier", $chromosome_name);
    }
    return $chromosome_item;
}   

sub make_protein {

    my ($proteins, $seq) = @_;
    my $protein_item;

    if (defined $proteins->{$seq}) {
        $protein_item = $proteins->{$seq};
    } else {
        $protein_item = make_item("Protein"); 
        $protein_item->set('sequence', make_seq($seq));
        $proteins->{$seq} = $protein_item;
    }
    return $protein_item;
}

sub make_exon {

    my ($exons, $primary_identifier) = @_;
    my $exon_item;

    if (defined $exons->{$primary_identifier}) {
        $exon_item = $exons->{$primary_identifier};
    } else {
        $exon_item = make_item("Exon"); 
        $exon_item->set('primaryIdentifier', $primary_identifier);
        $exons->{$primary_identifier} = $exon_item;
    }
    return $exon_item;
}


sub make_seq {

    my $seq = shift;

    my $seq_item = make_item("Sequence");
    $seq_item->set('residues', $seq);
    $seq_item->set('length', length($seq));

    return $seq_item;
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
