package DataDownloader::Source::InterProProtein;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => 'InterPro protein family domain and data',
    DESCRIPTION => "Protein Family and Domain data from Interpro",
    SOURCE_LINK => 'http://www.ebi.ac.uk/interpro',
    SOURCE_DIR  => 'interpro/match_complete',
    SOURCES => [

        {
            SUBTITLE => 'Match complete',
            HOST => 'ftp.ebi.ac.uk',
            REMOTE_DIR => 'pub/databases/interpro/Current',
            FILE => 'match_complete.xml.gz',
            EXTRACT => 1,
        },
        {
            SUBTITLE => 'Proteins to domains',
            HOST => 'ftp.ebi.ac.uk',
            REMOTE_DIR => 'pub/databases/interpro/Current',
            FILE => 'protein2ipr.dat.gz',
            EXTRACT => 1,
        },
    ],
};

1;
