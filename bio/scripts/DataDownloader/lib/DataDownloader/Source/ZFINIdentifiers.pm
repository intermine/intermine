package DataDownloader::Source::ZFINIdentifiers;

use Moose;
extends 'DataDownloader::Source::ABC';

# http://zfin.org/downloads/ensembl_1_to_1.txt
# ZFIN Marker associations to Ensembl IDs (Only associations with 1:1 
# relationships between ZFIN marker IDs and Ensembl IDs are stored in 
# ZFIN. All associations can be obtained using BioMart http://www.ensembl.org/biomart/martview)


use constant {
    TITLE       => 
        'ZFIN Identifiers',
    DESCRIPTION => 
        "ZFIN Marker synonyms and xrefs",
    SOURCE_LINK => 
        "http://zfin.org/",
    SOURCE_DIR => "zfin-identifiers",
};

sub BUILD {
    my $self = shift;
    $self->set_sources([
        {
            SERVER  => 'http://zfin.org/downloads',
            FILE    => "identifiersForIntermine.txt",
        },
    ]);
}

1;
