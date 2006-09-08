#!/usr/bin/perl -w

use strict;

BEGIN {
  push (@INC, ($0 =~ m:(.*)/.*:)[0] . '/../perl/lib');
}

use TIGR_XML_parser;
use Gene_obj;

use XML::Writer;
use InterMine::Item;
use InterMine::ItemFactory;
use InterMine::Model;

if (@ARGV < 4) {
  die "usage: $0 data_source taxon_id model_file tigr_xml_file_1 [tigr_xml_file_2 ...]\n";
}

my ($data_source, $taxon_id, $model_file, @files) = @ARGV;
my @items = ();
my $model = new InterMine::Model(file => $model_file);

my $item_factory = new InterMine::ItemFactory(model => $model);

my @items_to_write = ();

my $data_source_item = make_item("DataSource");
$data_source_item->set('name', $data_source);

my $data_set_item = make_item("DataSet");
$data_set_item->set('title', "$data_source data set taxon id: $taxon_id");

my $org_item = make_item("Organism");
$org_item->set("taxonId", $taxon_id);

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

sub make_synonym
{
  my ($subject, $type, $value, $data_source_item) = @_;

  my $syn = make_item("Synonym");
  $syn->set('subject', $subject);
  $syn->set('type', $type);
  $syn->set('value', $value);
  $syn->set('source', $data_source_item);
}

for my $file (@files) {
  my $TIGRparser = new TIGR_XML_parser();

  $TIGRparser->capture_genes_from_assembly_xml($file);

  my $chr_item = make_item("Chromosome");
  $chr_item->set("identifier", $TIGRparser->{chromosome});

  my @tigr_genes = $TIGRparser->get_genes();
  for my $tigr_gene (@tigr_genes) {
    $tigr_gene->refine_gene_object();

    #   use Data::Dumper;
    #   print Data::Dumper->Dump([$tigr_gene]);
    #   die;

    if ($tigr_gene->{is_pseudogene}) {
      next;
    }

    my $gene_item = make_item("Gene");
    $gene_item->set("organismDbId", $tigr_gene->{pub_locus});

    make_synonym($gene_item, 'identifier', $tigr_gene->{pub_locus}, $data_source_item);

    my $gene_loc = make_item("Location");
    my ($gene_end5, $gene_end3) = @{$tigr_gene->{gene_span}};
    $gene_loc->set('object', $chr_item);
    $gene_loc->set('subject', $gene_item);
    $gene_loc->set('start', $gene_end5);
    $gene_loc->set('end', $gene_end3);
    $gene_loc->set('evidence', [$data_set_item]);
  }
}

my $writer = new XML::Writer(DATA_MODE => 1, DATA_INDENT => 3);

$writer->startTag("items");

for my $item (@items_to_write) {
  $item->as_xml($writer);
}

$writer->endTag("items");
