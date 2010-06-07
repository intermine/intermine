#!/usr/bin/perl

# translates data from ensembl database to intermine items XML file

BEGIN {
    unshift (@INC, ($0 =~ m:(.*)/.*:)[0] . '/../../../intermine/perl/lib');    
}

use strict;
use warnings;
use Switch;

use XML::Writer;
use InterMine::Item;
use InterMine::ItemFactory;
use InterMine::Model;
use Bio::EnsEMBL::DBSQL::DBAdaptor;
use InterMine::Util qw(parse_properties_file);
use IO qw(Handle File);
use Cwd;
use Digest::MD5 qw(md5);

my $script_dir = ($0 =~ m:(.*)/.*:)[0];

if (@ARGV != 3) {
  die "usage: mine_name taxon_id data_destination \n"
     ."eg. flymine 9606 /shared/data/ensembl/current\n";  
}

# vars from the command line
my ($mine_name, $taxon_ids, $data_destination) = @ARGV;

# just for logging purposes
my $start_time = time();

# intermine
my $model_file = $script_dir . "/../../../$mine_name/dbmodel/build/main/genomic_model.xml";
my $properties = parse_properties_file("$ENV{HOME}/.intermine/$mine_name.properties");
my $model = new InterMine::Model(file => $model_file);
my $item_factory = new InterMine::ItemFactory(model => $model);

# init vars
my @items = (); # used to store all the items we make
my %locations = ();
my %organisms = parse_orgs($taxon_ids);

my $datasource = 'Ensembl';
my $datasource_item = make_item("DataSource");
$datasource_item->set('name', $datasource);

my ($org_item, $dataset_item);

# config file
my $config_file = $script_dir.'/../../sources/ensembl/resources/ensembl_config.properties';
%organisms = parse_config($config_file, %organisms);

foreach my $taxon_id (keys %organisms) {
    
    my (%proteins, %exons, %sequences, %locations);

    $org_item = make_item("Organism"); 
    $org_item->set("taxonId", $taxon_id);

    $dataset_item = make_item("DataSet");
    $dataset_item->set('name', "$datasource data set for taxon id: $taxon_id");

    my %chromosomes = ();
    my $chromosomes_string = $organisms{$taxon_id};

    if ($chromosomes_string) {
        %chromosomes = parse_chromosomes($chromosomes_string);
    }

    my $host    = $properties->{"db.ensembl.${taxon_id}.core.datasource.serverName"};
    my $dbname  = $properties->{"db.ensembl.${taxon_id}.core.datasource.databaseName"};
    my $user    = $properties->{"db.ensembl.${taxon_id}.core.datasource.user"};
    my $pass    = $properties->{"db.ensembl.${taxon_id}.core.datasource.password"};
    my $species = $properties->{"db.ensembl.${taxon_id}.core.datasource.species"};

    # This is all deprecated and needs adapting to Bio::EnsEMBL::Registry
    my $dbCore = Bio::EnsEMBL::DBSQL::DBAdaptor->new
        (-host => $host,
         -dbname => $dbname,
         -species => $species,
         -group => 'core', # This obviously needs to be changed if variations will be available
         -user => $user,
         -pass => $pass);
    
    my $slice_adaptor = $dbCore->get_sliceAdaptor();
    my @slices = @{$slice_adaptor->fetch_all('toplevel', undef, 0, 0)};

    my %processed_chromosomes = (); 

    for my $slice (@slices) {
        my $chromosome_name = $slice->seq_region_name();
		
#        if (($chromosomes_string && !exists $chromosomes{$chromosome_name}) || exists $processed_chromosomes{$chromosome_name}) { # why was this changed?
        if (($chromosomes_string && !exists $chromosomes{$chromosome_name})) {
            # skip this gene. it's not on a chromosome of interest
            next;
        }
	# add chromosome to list of chromosomes we've seen
	$processed_chromosomes{$chromosome_name}++; # This value isn't used any more

        my $chromosome_item = make_chromosome(\%chromosomes, $chromosome_name);
	
        my @genes =  @{$slice->get_all_Genes(undef,undef,1)};

        for my $gene (@genes) {

            my $gene_item = make_item("Gene");
            my $gene_type = $gene->biotype();
            
            $gene_item->set('featureType', $gene_type);
            
#            my $curated = 'false';
#            if ($gene->status eq 'KNOWN') {
#                $curated = 'true';
#            }
#            $gene_item->set('curated', $curated);
            
            parse_feature($gene, $gene_item, $chromosome_item); # fills in the info for gene_item
            make_synonym($gene_item, "identifier", $gene->stable_id());

            my @transcripts = @{ $gene->get_all_Transcripts() };
            for my $transcript (@transcripts) {
                
                my $transcript_item;
                if ($gene_type eq "protein_coding") {
                    $transcript_item = make_item("MRNA");
                } else {
                    $transcript_item = make_item("Transcript");
                }
                $transcript_item->set('gene', $gene_item);
                $transcript_item->set('sequence', make_seq(\%sequences, $transcript->seq->seq));
                # TODO are these transcripts going to be unique?
                make_synonym($transcript_item, "identifier", $transcript->stable_id());
                parse_feature($transcript, $transcript_item, $chromosome_item);
          
                if ($transcript->biotype() eq "protein_coding") {

                    my $translation = $transcript->translate();
                    if (!defined $translation) {
                        print "no translation for gene: " . $gene->stable_id() . "\n";
                        next;
                    }
                    my $protein_seq = $translation->seq();
                    my $protein_item = make_protein(\%proteins, \%sequences, $protein_seq);
                
                    $protein_item->set('genes', [$gene_item]);
                    $protein_item->set('transcripts', [$transcript_item]);
    
                    my $cds_item = make_item("CDS");
                    my $cds_primaryIdentifier = $transcript->stable_id() . "_CDS";

                    $cds_item->set('primaryIdentifier',$cds_primaryIdentifier);                    
                    $cds_item->set('sequence', make_seq(\%sequences, $transcript->translateable_seq()));
                    $cds_item->set('MRNA', $transcript_item);
                    $cds_item->set('protein', $protein_item);
                }
                                
                my @exons = @{ $transcript->get_all_Exons() };
                while ( my $exon = shift @exons ) {
                    my $primary_identifier = $exon->stable_id();
                    my $exon_item = make_exon(\%exons, $primary_identifier);
                    $exon_item->set('transcripts', [$transcript_item]);
                    $exon_item->set('gene', $gene_item);                    
                    $exon_item->set('sequence', make_seq(\%sequences, $exon->seq->seq));
                    parse_feature($exon, $exon_item, $chromosome_item);     
                }
            }
        }    
    }
    my $end_time = time();
    my $action_time = $end_time - $start_time;
    print "processing the files for $taxon_id took $action_time seconds to make "
         . scalar(@items) ." items; now creating the XML file... \n";
    
#write xml file # surely this is going to involve duplication if multiple taxon ids are passed
    $start_time = time();
    my $output = new IO::File(">$data_destination/$taxon_id.xml");
    my $writer = new XML::Writer(DATA_MODE => 1, DATA_INDENT => 3, OUTPUT => $output);
    $writer->startTag('items');
    for my $item (@items) {
        $item->as_xml($writer);
	#$item->destroy;
    }
    $writer->endTag('items');
    $writer->end();
    $output->close();
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
sub parse_feature {
    my ($feature, $item, $chromosome) = @_;    
    $item->set('primaryIdentifier', $feature->stable_id());    
    $item->set('chromosome', $chromosome);
    my $location = make_location($feature, $item, $chromosome);
    $item->set('chromosomeLocation', $location);
    return;
}

# read in the config file 
sub read_file { # Why is this not in the InterMine::Util module?
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
sub parse_config { # ARGGH! This was doing black magic, and fiddling with local variables!
    my ($file, %organisms) = @_;
    my @lines = read_file($file);
    foreach (@lines) {
        my $line = $_;
        my ($taxon_id, $config) = split('\.', $line);
        my ($label, $value) = split('=', $config);
        if ($label eq 'chromosomes' && defined $organisms{$taxon_id}) {
            $organisms{$taxon_id} = $value;
        }
    }
    return %organisms;
}

sub make_location {
    my ($feature, $item, $chromosome) = @_;
    my $location;

    my $start = $feature->start();
    my $end = $feature->end();
    my $itemId = $item->get('primaryIdentifier');
    my $chromosomeId = $chromosome->get('primaryIdentifier');

    my $key = $start . "|" . $end . "|" . $itemId . "|" . $chromosomeId;

    if (defined $locations{$key}) {
        $location = $locations{$key};
    } else {
        $location = make_item("Location");
        $location->set('start', $start);
        $location->set('end', $end);
        $location->set('strand', $feature->strand());
        $location->set('feature', $item);
        $location->set('locatedOn', $chromosome);
        $locations{$key} = $location;
    }
    return $location;
}

# user can specify which chromosomes to load  
# eg 1-21,X,Y
sub parse_chromosomes {
    my ($chromosome_string) = shift;
  
    my @bits = split(",", $chromosome_string);
    my %chromosomes;
 
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
  my $key = $subject . $type . $value;

  my $syn = make_item("Synonym");
  $syn->set('subject', $subject);
  $syn->set('type', $type);
  $syn->set('value', $value);
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

    my ($proteins, $sequences, $seq) = @_;
    my $protein_item;
    my $md5checksum = encodeSeq($seq);

    if (defined $proteins->{$md5checksum}) {
        $protein_item = $proteins->{$md5checksum};
    } else {
        $protein_item = make_item("Protein"); 
        $protein_item->set('sequence', make_seq($sequences, $seq));
        $proteins->{$md5checksum} = $protein_item;
        $protein_item->set('md5checksum', $md5checksum);
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
        make_synonym($exon_item, "identifier", $primary_identifier);
    }
    return $exon_item;
}


sub make_seq {

    my ($sequences, $seq) = @_;
    my $seq_item;

    my $md5checksum = encodeSeq($seq);

    if (defined $sequences->{$md5checksum}) {
        $seq_item = $sequences->{$md5checksum};
    } else {
        $seq_item = make_item("Sequence");
        $seq_item->set('residues', $seq);
        $seq_item->set('length', length($seq));
        $seq_item->set('md5checksum', $md5checksum);
        $sequences->{$md5checksum} = $seq_item;
    }
    return $seq_item;
}   

sub encodeSeq {

    my $seq = shift;

    my $ctx = Digest::MD5->new;
    $ctx->add($seq);
    return $ctx->hexdigest;
    
}


sub parse_orgs { # returns a hash with the keys set to the comma-delimited set of taxon ids
                 # and values all set to ''
    my $taxon_ids = shift;
    my %orgs;    
    for (split(',', $taxon_ids)) {
        $orgs{$_} = "";
    }
    return %orgs;
}

exit 0;
