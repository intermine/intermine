use strict;
use warnings;
use Switch;

BEGIN {
  # find the lib directory by looking at the path to this script
    push (@INC, ($0 =~ m:(.*)/.*:)[0] .'/../svn/dev/intermine/perl/lib/');
    }

#For Item handling and storing the item XML file 
use IO::File;
use XML::Writer;
use InterMine::Item;
use InterMine::ItemFactory;
use InterMine::Model;

#For getting data from EnsEMBL database
use Bio::EnsEMBL::Variation::DBSQL::DBAdaptor;
use Bio::EnsEMBL::DBSQL::DBAdaptor;



#Connect to EnsEMBL variation database
my $dbVariation = Bio::EnsEMBL::Variation::DBSQL::DBAdaptor->new
  (-host   => 'bob',
   -dbname => 'homo_sapiens_variation_50_36l',
   -species => 'homo_sapiens',
   -group   => 'variation',
   -user   => 'flymine',
   -pass => 'flymine');
#Conntect to EnsEMBL core database
my $dbCore = Bio::EnsEMBL::DBSQL::DBAdaptor->new
  (-host   => 'bob',
   -dbname => 'homo_sapiens_core_50_36l',
   -species => 'homo_sapiens',
   -group   => 'core',
   -user   => 'flymine',
   -pass => 'flymine');

my @items = ();
my $model_file = 'model.xml';
my $model = new InterMine::Model(file => $model_file);
my $item_factory = new InterMine::ItemFactory(model => $model);
my %sourceMap;
my %statesMap;
my %typeMap;

#:wqa
for (my $i=$ARGV[0]; $i<=$ARGV[1]; $i++) { 
	
	my @items_to_write = ();

	my @files;

	my $slice_adaptor = $dbCore->get_SliceAdaptor(); #get the database adaptor for Slice objects
	my $slice;
	switch($i) {
		case [1..22] {
			 $slice = $slice_adaptor->fetch_by_region('chromosome',$i); #get chromosome 21 in human
			}
		case 23 {
			 $slice = $slice_adaptor->fetch_by_region('chromosome','X'); #get chromosome 21 in human
			}
		case 24 {
			 $slice = $slice_adaptor->fetch_by_region('chromosome','Y'); #get chromosome 21 in human
			}
	}
	my $vf_adaptor = $dbVariation->get_VariationFeatureAdaptor(); #get adaptor to VariationFeature object
	my $vfs = $vf_adaptor->fetch_all_by_Slice($slice); #return ALL variations defined in $slice
	my $counter = 1;

	my $chromosome_item = make_item_chromosome(id => $i);
        $chromosome_item->set('primaryIdentifier', $slice->seq_region_name);

	foreach my $vf (@{$vfs}){
			#if ($counter == 100) {
				#last;
			#}
                        print "SNP NUMBER: ".$counter++." CHR:".$i."\n";
		  	my @alleles = split('[/.-]', $vf->allele_string);
                	if(!$alleles[0])        {
                        	$alleles[0]='-';
                	}
                	if(!$alleles[1])        {
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
	my $outfile ='/data/dg353/Chromosome'.$i.'.xml';
	my $output = new IO::File(">$outfile");
	my $writer = new XML::Writer(OUTPUT => $output, DATA_MODE => 1, DATA_INDENT => 3);
	$writer->startTag('items');
	for my $item (@items_to_write) {
		$item->as_xml($writer);
	}
	$writer->endTag('items');
	$writer->end();
	$output->close();

	sub make_item{
  		my $implements = shift;
  		my $item = $item_factory->make_item(implements => $implements);
		push @items_to_write, $item;
  		return $item;
	}

	sub make_item_chromosome{
		my %opts = @_;
                my $item = $item_factory->make_item(implements => 'Chromosome');
		$item->setPref(refId => $opts{id});
		push @items_to_write, $item;
                return $item;
        }

}



exit 0;
