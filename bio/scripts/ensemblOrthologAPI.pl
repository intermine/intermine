#!/usr/bin/perl

# translates data from ensembl Ortholog database to intermine items XML file

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
use Bio::EnsEMBL::Registry;
use Bio::EnsEMBL::Compara::DBSQL::DBAdaptor;


if (@ARGV != 3) {
  die "usage: mine_name taxon_ids data_destination \n eg. flymine 9606 /shared/data/ensembl/current\n";  
}

my ($mine_name, $taxon_ids, $data_destination) = @ARGV;
my $taxon_id=$taxon_ids;
my $start_time = time();

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

my $config_file = '../sources/ensembl/resources/ensembl_config.properties';
parse_config(read_file($config_file));

my $properties_file = "$ENV{HOME}/.intermine/$mine_name.properties";



my %proteins = ();
my %exons = ();
my %sequences = ();
my %genes = ();
my %orgs = ();

$dataset_item = make_item("DataSet");
$dataset_item->set('title', "$datasource data set for taxon id: $taxon_id");

my %chromosomes = ();
my $chromosomes_string = $organisms{$taxon_id};

if ($chromosomes_string) {
    %chromosomes = parse_chromosomes($chromosomes_string);
}

my $host = 'localhost';
my $dbname = 'ensembl_compara_55_37';
my $user = 'flymine';
my $pass = 'flymine';

my $comparaDB = Bio::EnsEMBL::Compara::DBSQL::DBAdaptor->new
    (-host => $host,
     -dbname => $dbname,
     -group => 'core',
     -user => $user,
     -pass => $pass);

    my $dbCore = Bio::EnsEMBL::DBSQL::DBAdaptor->new
        (-host => 'localhost',
         -dbname => 'homo_sapiens_core_55_37',
         -species => '9606',
         -group => 'core',
         -user => 'flymine',
         -pass => 'flymine');

    
# First things first - dig out all the genes...
# Just for the record, Perl has more problems than I can count, but for starters:
# 1) Homogenising data types. Not clever, but dumb.
# 2) Throwing incomprehensible generic errors. I'd be ashamed of Perl's output in my private scripts it's so amateurish.
# 3) Speed. For the love of God, why bother with something so slow if it has none of the advantages of interpreted languages?

# Also for the record, whoever wrote the Ensembl API should be hung, drawn, quartered and shot repeatedly.
# Admittedly, this is coming from a Mac programmer; the Apple APIs are masterpieces of elegance, power and simplicity
# Not to mention that their documentation is...fantastic. Unlike Ensembl docs which have multiple errors per line.

# Reader beware; there may be a better way of doing the following, but if you can find it, you are a better person than I.
# As of 13/7/09, this is the best conceivable method that can be gleaned from Ensembl docs without rewriting API myself (seriously considered)

# Idea is:
# 1) Use slice to get all genes
# 2) Get ensembl IDs from genes ($gene->stable_id())
# 3) For each gene ID use homology adaptor to get all homologies
# 4) For each homology loop through the "members" of the homlogy (homogeneity ftw)
# 5) Use Intermine API to make Intermine homology objects
# 6) Dump to XML file

# P.s. - macs don't have a hash key. Find a new symbol for comments!

my $slice_adaptor = $dbCore->get_sliceAdaptor();
my @slices = @{$slice_adaptor->fetch_all('toplevel')};

# my $reg = "Bio::EnsEMBL::Registry"; 
# $reg->load_registry_from_db( -host=>$host, -user => $user,-group => 'core',-dbname=>$dbname,-pass=>$pass); 
# my $member_adaptor = $reg->get_adaptor("Multi", "compara", "Member");
# my $homology_adaptor = $reg->get_adaptor('Multi', 'compara', 'Homology');

# Bio::EnsEMBL::Registry->load_registry_from_db(-host=>'localhost', -user => 'flymine',-dbname=>'ensembl_compara_55_37',-pass=>'flymine');
Bio::EnsEMBL::Registry->load_registry_from_db(
    -host => 'ensembldb.ensembl.org', -user => 'anonymous');

my $member_adaptor = Bio::EnsEMBL::Registry->get_adaptor('Multi','compara','Member');
my $member = $member_adaptor->fetch_by_source_stable_id('ENSEMBLGENE','ENSG00000004059');
print $member->stable_id;

my $homology_adaptor = Bio::EnsEMBL::Registry->get_adaptor('Multi', 'compara', 'Homology');


my $sliceSize=@slices;

my $numSlices=0;
my $numGenes=0;

while (my $slice = shift @slices) {
	$numSlices++;
	$numGenes=0;

	my @orgs;
	
        my $chromosome_name = $slice->seq_region_name();

        my $chromosome_item = make_chromosome(\%chromosomes, $chromosome_name);

        my @genes =  @{$slice->get_all_Genes(undef,undef,1)};
	my $geneSize=@genes;
        while (my $gene = shift @genes) {
		$numGenes++;
		print $gene->stable_id;

	        my $member = $member_adaptor->fetch_by_source_stable_id("ENSEMBLGENE", $gene->stable_id);
		print "Parsing ", $gene->stable_id,"; Slices: ", $numSlices, "/", $sliceSize, "; Genes: ", $numGenes, "/", $geneSize, "\n";
		if (defined($member)) {
			my $homologies = $homology_adaptor->fetch_all_by_Member($member);
			foreach my $homology (@{$homologies}) 
			{

				my $completeHomologue = make_item("Homologue");

				my ($gene1, $gene2) = @{$homology->gene_list};

				my $geneItem = make_gene(\%genes,$gene1->stable_id,$gene1->taxon_id,\%orgs);
				my $homoItem = make_gene(\%genes, $gene2->stable_id,$gene2->taxon_id,\%orgs);

				# Now make the homologue:

				$completeHomologue->set('gene',$geneItem);
				$completeHomologue->set('homologue',$homoItem);
				if (defined($homology->dnds_ratio))
				{
					$completeHomologue->set('DnDsRatio',$homology->dnds_ratio);
					print $homology->dnds_ratio;
					print "\n";
				}
				if (defined($homology->description)) 
				{
					$completeHomologue->set('type',$homology->description);
				}
				else {
					print "No  description!!\n";
				}

			}
		}
	}
}

my $end_time = time();
my $action_time = $end_time - $start_time;
print "processing the files for $taxon_id took $action_time seconds.  now creating the XML file... \n";

#write xml file
$start_time = time();
my $output = new IO::File(">$data_destination/output.xml");
my $writer = new XML::Writer(DATA_MODE => 1, DATA_INDENT => 3, OUTPUT => $output);
$writer->startTag('items');

for my $item (@items) {

    # print $item->as_xml();
    $item->as_xml($writer);
}
$writer->endTag('items');

$writer->end();
$output->close();
$end_time = time();
$action_time = $end_time - $start_time;
print "creating the XML file took $action_time seconds.\n";


# helper method that makes a new object of a particular class and saves it in 
# the @items array
sub make_item {

    my ($implements, $taxID,$orgs) = @_;
    # my $implements = shift;
    my $org_item;
    my $item = $item_factory->make_item(implements => $implements);
    push @items, $item;
    if ($item->valid_field('organism')) {
	if (defined($taxID)) 
	{

# print "tax ID defined...\n";

	    if (defined($orgs->{$taxID})) {

# print "Exists: ", $taxID, " - using...",$orgs->{$taxID},"\n";

		$org_item=$orgs->{$taxID};
		$item->set('organism',$org_item);
	    } else {

		$org_item = make_item("Organism");
		$org_item->set("taxonId", $taxID);
		$orgs->{$taxID} = $org_item;
		make_synonym($org_item,"identifier",$taxID);
		$item->set('organism',$orgs->{$taxID});

# print "Doesn't exist: ", $taxID,$orgs->{$taxID}, "; creating...\n";
	    }

        }
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

sub make_gene {
	my ($genes, $gene_name, $taxID, $orgs) = @_;
	my $gene_item;

	if (defined $genes->{$gene_name}) {
		$gene_item = $genes->{$gene_name};
	} else {
		$gene_item = make_item("Gene",$taxID,$orgs);
		$gene_item->set('primaryIdentifier',$gene_name);
		$genes->{$gene_name} = $gene_item;
		make_synonym($gene_item,"identifier",$gene_name);
	}
	return $gene_item;
}

sub make_homologue {
	my ($homologues, $homologue_name) = @_;
	my $homologue_item;

	if (defined $homologues->{$homologue_name}) {
		$homologue_item = $homologues->{$homologue_name};
	} else {
		$homologue_item = make_item("Homologue");
		$homologue_item->set('primaryIdentifier',$homologue_name);
		$homologues->{$homologue_name} = $homologue_item;
		make_synonym($homologue_item,"identifier",$homologue_name);
	}
	return $homologue_item;
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


sub parse_orgs {
    my ($taxon_ids) = @_;
    my %orgs = ();    
    for (split("\\,", $taxon_ids)) {
        $orgs{$_} = "";
    }
    return %orgs;
}

exit 0;
