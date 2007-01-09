#!/usr/bin/perl -w

use strict;

BEGIN {
  # find the lib directory by looking at the path to this script
  push (@INC, ($0 =~ m:(.*)/.*:)[0] . '/../../intermine/perl/lib');
}

use XML::Writer;
use InterMine::Item;
use InterMine::ItemFactory;
use InterMine::Model;

if (@ARGV < 3) {
  die "usage: $0 data_source taxon_id model_file [files...]\n";
}

my ($data_source, $taxon_id, $model_file, @files) = @ARGV;
my @items = ();
my $model = new InterMine::Model(file => $model_file);

my $item_factory = new InterMine::ItemFactory(model => $model);

my @items_to_write = ();

my $org_item = make_item('Organism');
$org_item->set('taxonId', $taxon_id);

sub make_item
{
  my $implements = shift;
  my $item = $item_factory->make_item(implements => $implements);
  push @items_to_write, $item;
  if ($item->valid_field('organism')) {
    $item->set('organism', $org_item);
  }
  return $item;
}

my $data_source_item = make_item('DataSource');
$data_source_item->set('name', $data_source);

my $data_set_item = make_item('DataSet');
$data_set_item->set('title', "$data_source data set taxon id: $taxon_id");

# make a protein and add two publications to its evidence collection
my $protein1_item = make_item('Protein');
$protein1_item->set('primaryAccession', 'Q8I5D2');

my $protein2_item = make_item('Protein');
$protein2_item->set('primaryAccession', 'Q8I4X0');

my $gene1_item = make_item('Gene');
$gene1_item->set('identifier', 'gene 1');

my $gene2_item = make_item('Gene');
$gene2_item->set('identifier', 'gene 2');

$protein1_item->set('genes', [$gene1_item, $gene2_item]);
$protein2_item->set('genes', [$gene1_item, $gene2_item]);

my $pub1_item = make_item('Publication');
$pub1_item->set('pubMedId', 12368864);

my $pub2_item = make_item('Publication');
$pub2_item->set('pubMedId', 16248207);

# set a collection - no reverse reference
$protein1_item->set('evidence', [$pub1_item, $pub2_item]);

my $protein_region_item = make_item('ProteinRegion');
$protein_region_item->set('curated', 'true');
$protein_region_item->set('identifier', 'protein_region_1');

# set a collection and automatically set the reverse reference
$protein1_item->set('regions', [$protein_region_item]);


my $writer = new XML::Writer(DATA_MODE => 1, DATA_INDENT => 3);
$writer->startTag('items');
for my $item (@items_to_write) {
  $item->as_xml($writer);
}
$writer->endTag('items');
