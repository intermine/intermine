package DataDownloader::Source::Reactome;

use Moose;
extends 'DataDownloader::Source::ABC';
use DataDownloader::Util qw(make_link);
use File::Path qw(mkpath);

use constant {
    TITLE          => 
        'Reactome',
    DESCRIPTION    => 
        'Curated knowledgebase of biological pathways in humans.',
    SOURCE_LINK    => 
        'http://www.reactome.org',
    SOURCE_DIR     => 
       'reactome', 
    SOURCES => [{
        SERVER  => "http://reactome.org/download/current",
        FILE    => 'biopax.zip',
    }],
};
use constant ORGANISMS => (
    "Drosophila melanogaster", 
    "Caenorhabditis elegans",
    "Mus musculus",
    "Homo sapiens",
    "Escherichia coli",
    "Bacillus subtilis",
    "Arabidopsis thaliana",
);

override clean_up => sub {
    my $self = shift;
    my @wanted_members = map {"$_.owl"} ORGANISMS;
    my $not_curated = $self->get_destination_dir->subdir("not-curated");
    my $curated = $self->get_destination_dir->subdir("curated");
    my $curated_ln = $self->get_source_dir->subdir("curated");
    for my $d ($not_curated, $curated) {
        mkpath $d unless (-d $d);
    }
    make_link($curated, $curated_ln);
    # Unzip the files we want into the not-curated directory
    my @args = ( 'unzip', 
        $self->get_destination_dir->file("biopax.zip"), 
        @wanted_members, 
        '-d', $not_curated );
    $self->execute_system_command(@args);
    unlink($self->get_destination_dir->file("biopax.zip"));
};

1;
