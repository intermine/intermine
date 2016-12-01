package DataDownloader::Source::InterPro;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => 'InterPro domain data',
    DESCRIPTION => "Protein Family and Domain data from Interpro",
    SOURCE_LINK => 'http://www.ebi.ac.uk/interpro',
    SOURCE_DIR  => 'interpro',
    SOURCES => [
        {
            SUBTITLE => 'Domains',
            HOST => 'ftp.ebi.ac.uk',
            REMOTE_DIR => 'pub/databases/interpro',
            FILE => 'interpro.xml.gz',
            EXTRACT => 1,
        },
    ],
};

1;
