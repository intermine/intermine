package DataDownloader::Source::UniProtDataSources;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE       => 
        'UniProt Data Sources',
    DESCRIPTION => 
        "Data sources from UniProt",
    SOURCE_LINK => 
        "http://www.uniprot.org",
    SOURCE_DIR => "uniprot/xrefs",
    SOURCES => [
        {
            SUBTITLE => 'UniProt',
            HOST => "ftp.uniprot.org",
            REMOTE_DIR => "pub/databases/uniprot/knowledgebase/docs",
            FILE => "dbxref.txt",
            EXTRACT => 0,
        },
    ],
};

1;
