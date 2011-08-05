package DataDownloader::Source::PubMed;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => "PubMed",
    DESCRIPTION => "Gene information from PubMed and publications which mention them from NCBI",
    SOURCE_LINK => "http://www.ncbi.nlm.nih.gov/",
    SOURCE_DIR => 'pubmed',
    SOURCES => [
        {
            SUBTITLE => 'NCBI',
            HOST => "ftp.ncbi.nlm.nih.gov",
            REMOTE_DIR => "gene/DATA",
            FILE => "gene_info.gz",
            EXTRACT => 1,
        },
        {
            SUBTITLE => 'NCBI',
            HOST => "ftp.ncbi.nlm.nih.gov",
            REMOTE_DIR => "gene/DATA",
            FILE => "gene2pubmed.gz",
            EXTRACT => 1,
        },
    ],
};





