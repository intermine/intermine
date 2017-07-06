package DataDownloader::Source::MGIIdentifiers;

use Moose;
extends 'DataDownloader::Source::ABC';

use constant {
    TITLE => 'MGI Identifiers',
    DESCRIPTION => 'MGI Identifiers',
    SOURCE_LINK => 'http://www.informatics.jax.org',
    SOURCE_DIR => 'mgi-identifiers',
    SOURCES => [
        {
            SERVER => 'http://www.informatics.jax.org/downloads/reports',            
            FILE => 'MRK_List2.rpt',
        },
    ],
};

1;