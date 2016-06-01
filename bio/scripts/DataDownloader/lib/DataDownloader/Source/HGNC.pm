package DataDownloader::Source::HGNC;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => 'HGNC',
    DESCRIPTION => 'HGNC gene identifiers',
    SOURCE_LINK => 'http://www.ncbi.nlm.nih.gov/homologene',
    SOURCE_DIR => 'human/hgnc',
    SOURCES => [
        {
            HOST => 'ftp://ftp.ebi.ac.uk',
            REMOTE_DIR => '/pub/databases/genenames/',
            FILE => 'hgnc_complete_set.txt.gz',
            EXTRACT => 1,
        },
    ],
};

1;
