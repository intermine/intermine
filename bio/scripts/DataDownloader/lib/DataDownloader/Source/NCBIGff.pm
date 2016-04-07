package DataDownloader::Source::NCBIGff;

use Moose;
extends 'DataDownloader::Source::FtpBase';


use constant {
    TITLE => "NCBI Gene ",
    DESCRIPTION => "GFF from NCBI",
    SOURCE_LINK => "ftp.ncbi.nih.gov",
    SOURCE_DIR => "ncbi",
    SOURCES => [{
        FILE => "ref_GRCh38.p2_top_level.gff3.gz", 
        HOST => "ftp.ncbi.nih.gov",
        REMOTE_DIR => "genomes/H_sapiens/GFF",
        EXTRACT => 1,
    }],
};

1;
