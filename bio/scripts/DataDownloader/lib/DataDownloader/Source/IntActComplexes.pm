package DataDownloader::Source::IntActComplexes;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => "IntAct Complexes",
    DESCRIPTION => "Complex protein interaction data from IntAct",
    SOURCE_LINK => "http://www.ebi.ac.uk/intact",
    SOURCE_DIR => 'psi/intact/complexes',
    SOURCES => [
        {
            SUBTITLE => 'Intact',
            HOST => "ftp.ebi.ac.uk",
            REMOTE_DIR => "pub/databases/IntAct/complex/current/psi25/Homo_sapiens",
            FILE => "variant_summary.txt.gz",
            EXTRACT => 1,
        },
    ],
};
