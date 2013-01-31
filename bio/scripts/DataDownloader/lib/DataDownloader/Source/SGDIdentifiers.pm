package DataDownloader::Source::SGDIdentifiers;

use Moose;
extends 'DataDownloader::Source::ABC';

# http://downloads.yeastgenome.org/curation/chromosomal_feature/SGD_features.tab
# Chromosomal features in SGD, including coordinates and subfeatures.

use constant {
    TITLE       => 
        'SGD Identifiers',
    DESCRIPTION => 
        "Chromosomal features in SGD",
    SOURCE_LINK => 
        "http://www.yeastgenome.org/",
    SOURCE_DIR => "sgd-identifiers",
};

sub BUILD {
    my $self = shift;
    $self->set_sources([
        {
            SERVER  => 'http://downloads.yeastgenome.org/curation/chromosomal_feature',
            FILE    => "SGD_features.tab",
        },
    ]);
}

1;