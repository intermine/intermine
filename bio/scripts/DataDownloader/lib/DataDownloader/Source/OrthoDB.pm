package DataDownloader::Source::OrthoDB;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => 'OrthoDB',
    DESCRIPTION => 'OrthoDB Homologues',
    SOURCE_LINK => 'http://cegg.unige.ch/orthodb',
    SOURCE_DIR => 'orthodb',
    SOURCES => [
        {
            HOST => 'cegg.unige.ch',
            REMOTE_DIR => '/OrthoDB6/',
            FILE => 'OrthoDB6_ALL_METAZOA_tabtext.gz',
            EXTRACT => 1,
        },
        {
            HOST => 'cegg.unige.ch',
            REMOTE_DIR => '/OrthoDB6/',
            FILE => 'OrthoDB6_ALL_FUNGI_tabtext.gz',
            EXTRACT => 1,
        },
    ],
};

1;
