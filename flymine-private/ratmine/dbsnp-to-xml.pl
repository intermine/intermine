#!/usr/bin/perl
# dbsnp-to-xml.pl
# purpose: to create a target items xml file for intermine from dbSNP Chromosome Report file
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
use XML::XPath;
use Cwd;

my ($model_file, $input_directory, $output_directory) = @ARGV;

unless ( $model_file ne '' and -e $model_file)
{
	print "\ndbsnp-to-xml.pl\n";
	print "Convert the dbSNP XML file into InterMine XML\n";
	print "dbsnp-to-xml.pl model_file input_xml_file output_xml_file\n\n";
	exit(0);
}

my @files = <${input_directory}*.xml>;
my $model = new InterMine::Model(file => $model_file);
my $item_factory = new InterMine::ItemFactory(model => $model);

my $org_item = $item_factory->make_item('Organism');
my $taxon_id = 10116;
$org_item->set('taxonId', $taxon_id);
my $dataset_item = $item_factory->make_item('DataSet');
my $organism_trigger = 0;
my $dataset_trigger = 0;
my %consequences;

my $count = 0;
while(my $file = pop(@files))
{
	&processDbSNPFile($file);
}#end while

exit(0);

###Subroutines###

sub processDbSNPFile
{
	my $file = shift;
	my $data_source = 'dbSNP';
	
	my $outfile = $1 if $file =~ m|(\w+\.xml)$|;
	my $output = new IO::File(">${output_directory}intermine_$outfile") or die "cannot open IO::Steam >${output_directory}intermine_$outfile";
	my $writer = new XML::Writer(DATA_MODE => 1, DATA_INDENT => 3, OUTPUT => $output);


	# The item factory needs the model so that it can check that new objects have
	# valid classnames and fields
	$writer->startTag("items");
	unless($organism_trigger)
	{
		$org_item->as_xml($writer);
		$organism_trigger = 1;
	}

	# read the genes file
	open SNP, $file;
	my %index;
	my %chromosomes;
	my $count = 0;
	my $entry = '';
	print "processing $file\n";
	while(<SNP>)
	{
		chomp;
		$entry .= $_;

		#create the dataset and datasource objects
		if(!$dataset_trigger and $entry =~ /dbSnpBuild="(\d+)"\s+generated="([\d\D]+?)"/) 
		{

			my ($build, $date) = ($1, $2);
			$dataset_item->set('title', "dbSNP build:$build, $date");
			$dataset_item->as_xml($writer);
			$dataset_trigger = 1;
		}
	
		#find one SNP at a time
		if( $entry =~ m|<Rs[\d\D]+?</Rs>|) #parses header line
		{
			
			#print "$entry\n"; exit (0);
			#once found load into XPATH object to parse out data
			my $xp = XML::XPath->new(xml => $&);

			my $snp_item = $item_factory->make_item('rsSNP');
			$snp_item->set('dataSets', [$dataset_item]);
			#find Rs Id
			my $id = $xp->find('//Rs/@rsId')->string_value;
			$snp_item->set('primaryIdentifier', "rs$id");
			$snp_item->set('organism', $org_item);
		
			#find chromosome
			my $chrom = $xp->find('//Assembly[@groupLabel="RGSC_v3.4"]/Component/@chromosome')->string_value;
			my $chromosome_item;
			if($chromosomes{$chrom})
			{
				$chromosome_item = $chromosomes{$chrom};
			}
			else
			{
				$chromosome_item = $item_factory->make_item('Chromosome');
				$chromosome_item->set('primaryIdentifier', $chrom);
				$chromosomes{$chrom} = $chromosome_item;
				$chromosome_item->as_xml($writer);
			}

			#find consequence/function
			my $fxnClass = $xp->find('//Assembly[@groupLabel="RGSC_v3.4"]/Component/MapLoc/FxnSet/@fxnClass')->string_value;
			
			my $consequences = &getConsequenceType($fxnClass, $writer);
			$snp_item->set('consequenceTypes', [$consequences]) if ($consequences);
		
			#set chromosome and location
			my $pos = $xp->find('//Assembly[@groupLabel="RGSC_v3.4"]/Component/MapLoc/@physMapInt')->string_value;
			my $orient = $xp->find('//Assembly[@groupLabel="RGSC_v3.4"]/Component/MapLoc/@orient')->string_value;
			if($orient eq 'forward')
			{	$pos++; }
			elsif($orient eq 'reverse')
			{	$pos--; }
			my $loc_item = $item_factory->make_item('Location');
			$loc_item->set('object', $chromosome_item);
			$loc_item->set('start', $pos);
			$loc_item->set('end', $pos);
			$loc_item->set('subject', $snp_item);
			$loc_item->as_xml($writer);
			$snp_item->set('chromosome', $chromosome_item);
			$snp_item->set('chromosomeLocation', $loc_item);
			
			#set rsSequence
			my $rs5 = $xp->find('/Rs/Sequence/Seq5')->string_value;
			my $rsAllele = $xp->find('/Rs/Sequence/Observed')->string_value;
			my $rs3 = $xp->find('/Rs/Sequence/Seq3')->string_value;

			$snp_item->set('fivePrimeSequence', $rs5);
			$snp_item->set('allele', $rsAllele);
			$snp_item->set('threePrimeSequence', $rs3);
		
			#create ssSNPs
			my %ssSNPs;
			my $ssSet = $xp->find('/Rs/Ss');
			foreach my $node ($ssSet->get_nodelist)
			{
				my $ss_item = $item_factory->make_item('ssSNP');
				my $ssId = $node->find('@ssId')->string_value;
				$ss_item->set('primaryIdentifier', "ss$ssId");
				$ss_item->set('organism', $org_item);
				my $five = $node->find('Sequence/Seq5')->string_value;
				my $three = $node->find('Sequence/Seq3')->string_value;
				my $allele = $node->find('Sequence/Observed')->string_value;
				$ss_item->set('fivePrimeSequence', $five);
				$ss_item->set('threePrimeSequence', $three);
				$ss_item->set('allele', $allele);
				$ss_item->set('rsSNP', $snp_item);
				$ss_item->set('chromosome', $chromosome_item);
				$ss_item->set('chromosomeLocation', $loc_item);

				#Submitter SNP ID as Synonym
				my $submitted_id = $node->find('@locSnpId')->string_value;
				my $syn_item = $item_factory->make_item('Synonym');
				$syn_item->set('value', $submitted_id);
				$syn_item->set('type', 'Submitter SNP ID');

				my $syn_item2 = $item_factory->make_item('Synonym');
				$syn_item2->set('value', "ss$ssId");
				$syn_item2->set('type', 'dbSNP ss Identifier');
				$syn_item2->set('subject', $ss_item);

				$ss_item->set('synonyms', [$syn_item, $syn_item2]);

				$ss_item->as_xml($writer);
				$syn_item->as_xml($writer);
				$ssSNPs{$ssId} = $ss_item;
			}
		
			#relate ssSNPs to rsSNP
			$snp_item->set('ssSNPs', [values(%ssSNPs)]);
			my $exSNP = $xp->find('//Rs/Sequence/@exemplarSs')->string_value;
			$snp_item->set('exemplarSNP', $ssSNPs{$exSNP});

			my $syn_item = $item_factory->make_item('Synonym');
			$syn_item->set('value', "rs$id");
			$syn_item->set('type', "dbSNP rs Identifier");
			$syn_item->set('subject', $snp_item);
			$syn_item->set('isPrimary', 'true');
			$syn_item->as_xml($writer);
			$snp_item->set('synonyms', [$syn_item]);
			
		
			$snp_item->as_xml($writer);
			$loc_item = $loc_item->destroy;
			$snp_item = $snp_item->destroy;
			$entry = ''; #empty $entry
		}

	}#end while
	close SNP;

	$writer->endTag("items");
}

sub getConsequenceType
{
	my ($fxnClass, $writer) = @_;
	if ($consequences{$fxnClass}) 
	{
		return $consequences{$fxnClass};
	}
	elsif($fxnClass)
	{
		my $consequence_item = $item_factory->make_item('ConsequenceType');
		$consequence_item->set('type', $fxnClass);
		$consequences{$fxnClass} = $consequence_item;
		$consequence_item->as_xml($writer);
		return $consequences{$fxnClass};
	}
	return undef;
}