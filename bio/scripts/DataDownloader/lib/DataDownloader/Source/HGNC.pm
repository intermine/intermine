package DataDownloader::Source::HGNC;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => 'HGNC',
    DESCRIPTION => 'HGNC gene identifiers',
    SOURCE_LINK => 'http://www.ebi.ac.uk',
    SOURCE_DIR => 'human/hgnc',
    SOURCES => [
        {
            HOST => 'ftp.ebi.ac.uk',
            REMOTE_DIR => 'pub/databases/genenames/new/tsv',
            FILE => 'hgnc_complete_set.txt',
            EXTRACT => 0,
        },
    ],
};

1;
