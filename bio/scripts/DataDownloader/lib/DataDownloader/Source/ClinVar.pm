package DataDownloader::Source::ClinVar;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => "ClinVar",
    DESCRIPTION => "Clinical variations",
    SOURCE_LINK => "http://www.ncbi.nlm.nih.gov/",
    SOURCE_DIR => 'human/clinvar',
    SOURCES => [
        {
            SUBTITLE => 'NCBI',
            HOST => "ftp.ncbi.nlm.nih.gov",
            REMOTE_DIR => "pub/clinvar/tab_delimited",
            FILE => "variant_summary.txt.gz",
            EXTRACT => 1,
        },
    ],
};
