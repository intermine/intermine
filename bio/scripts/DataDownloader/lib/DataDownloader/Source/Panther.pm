package DataDownloader::Source::FlyCVTerms;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => 'Panther',
    DESCRIPTION => 'Panther Homologues',
    SOURCE_LINK => 'http://www.pantherdb.org',
    SOURCE_DIR => 'panther',
    SOURCES => [
        {
            HOST => 'ftp://ftp.pantherdb.org',
            REMOTE_DIR => '/ortholog/current/',
            FILE => 'RefGenomeOrthologs.tar.gz',
            EXTRACT => 1,
        },
    ],
};

1;
