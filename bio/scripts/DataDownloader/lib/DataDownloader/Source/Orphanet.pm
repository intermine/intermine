package DataDownloader::Source::Orphanet;

use Moose;
extends 'DataDownloader::Source::ABC';

use constant {
    TITLE  => 'Orphanet',
    DESCRIPTION => "Diseases from Orphanet",
    SOURCE_LINK => "http://www.orphadata.org",
    SOURCE_DIR => 'human/orphanet',
    SOURCES => [{
        FILE   => 'en_product1.xml',
        SERVER => 'http://www.orphadata.org/data/xml/',
    }],
};

1;
