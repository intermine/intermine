#!/usr/bin/perl -w

use strict;

BEGIN {
  push (@INC, ($0 =~ m:(.*)/.*:)[0] . '/../../perl/lib');
}

use TIGR_XML_parser;
use Gene_obj;

use XML::Writer;
use InterMine::Item;
use InterMine::ItemFactory;
use InterMine::Model;

if (@ARGV != 2) {
  die "usage: $0 model_file tigr_xml_file\n";
}

my ($model_file, $file) = @ARGV;
my @items = ();
my $model = new InterMine::Model(file => $model_file);

my $item_factory = new InterMine::ItemFactory(model => $model);

my $writer = new XML::Writer(DATA_MODE => 1, DATA_INDENT => 3);
my $TIGRparser = new TIGR_XML_parser();

$TIGRparser->capture_genes_from_assembly_xml($file);

$writer->startTag("items");

my @tigr_genes = $TIGRparser->get_genes();
foreach my $tigr_gene (@tigr_genes) {
  $tigr_gene->refine_gene_object();

  my $gene_item = $item_factory->make_item("Gene");
  $gene_item->set("organismDbId", $tigr_gene->{pub_locus});

  $gene_item->as_xml($writer);
}

$writer->endTag("items");
