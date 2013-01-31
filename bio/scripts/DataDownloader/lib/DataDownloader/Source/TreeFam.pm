package DataDownloader::Source::TreeFam;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => 'TreeFam',
    DESCRIPTION => 'TreeFam Phylogenetic tree families database',
    SOURCE_LINK => 'http://www.treefam.org',
    SOURCE_DIR => 'treefam',
    SOURCES => [
        {
            HOST => 'ftp.sanger.ac.uk',
            REMOTE_DIR => '/pub/treefam/release-current/MySQL/',
            FILE => 'ortholog.txt.table.gz',
            EXTRACT => 1,
        },

        {
            HOST => 'ftp.sanger.ac.uk',
            REMOTE_DIR => '/pub/treefam/release-current/MySQL/',
            FILE => 'genes.txt.table.gz',
            EXTRACT => 1,
        },
    ],
};

1;
