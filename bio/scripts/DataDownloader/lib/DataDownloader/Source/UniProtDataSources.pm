package DataDownloader::Source::UniProtDataSources;

use Moose;
extends 'DataDownloader::Source::ABC';

use constant {
    TITLE       => 
        'UniProt Data Sources',
    DESCRIPTION => 
        "Data sources from UniProt",
    SOURCE_LINK => 
        "http://www.uniprot.org",
    SOURCE_DIR => "uniprot/xrefs/",
};

sub BUILD {
    my $self = shift;
    $self->set_sources([
        {
            SERVER  => 'http://www.uniprot.org/docs/',
            FILE    => "dbxref.txt",
        },
    ]);
}

1;
