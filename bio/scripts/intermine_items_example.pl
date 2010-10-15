#!/usr/bin/perl

# every perl script should start with these two lines.
use strict;
use warnings;

use InterMine::Item::Document;
use InterMine::Model;

if (@ARGV < 3) {
  die "usage: $0 data_source taxon_id model_file [files...]\n";
}

my ($data_source, $taxon_id, $model_file, @files) = @ARGV;

my $model = new InterMine::Model(file => $model_file);
my $doc = new InterMine::Item::Document(model => $model);

my $org_item = make_item(
    Organism => (
        taxonId => $taxon_id,
    )
);
my $data_source_item = make_item(
    DataSource => (
        name => $data_source,
    ),
);

my $data_set_item = make_item(
    DataSet => (
        name => "$data_source data set taxon id: $taxon_id",
    ),
);

# make a protein and add two publications to its publications collection
my $protein1_item = make_item(
    Protein => (
        primaryAccession => 'Q8I5D2',
    ),
);

my $protein2_item = make_item(
    Protein => (
        primaryAccession => 'Q8I4X0',
    ),
);

my $gene1_item = make_item(
    Gene => (
        primaryIdentifier => 'gene 1',
    ),
);
my $gene2_item = make_item(
    Gene => (
        primaryIdentifier => 'gene 2',
    ),
);

$protein1_item->set(genes => [$gene1_item, $gene2_item]);
$protein2_item->set(genes => [$gene1_item, $gene2_item]);

my @pubmed_ids = (12368864, 16248207);
my @pubs = map {make_item(Publication => (pubMedId => $_))} @pubmed_ids;

# set a collection - no reverse reference
$protein1_item->set(publications => \@pubs);

$doc->close(); # writes the xml
exit(0);

######### helper subroutines:

sub make_item {
    my @args = @_;
    my $item = $doc->add_item(@args);
    if ($item->valid_field('organism')) {
        $item->set(organism => $org_item);
    }
    return $item;
}
