package DataDownloader::Source::NCBIGene;

use Moose;
extends 'DataDownloader::Source::FtpBase';

# ftp://ftp.ncbi.nih.gov/gene/DATA/GENE_INFO/Mammalia/Homo_sapiens.gene_info.gz

use constant {
    TITLE => "NCBI Gene ",
    DESCRIPTION => "Genes from NCBI",
    SOURCE_LINK => "ftp.ncbi.nih.gov",
    SOURCE_DIR => "ncbi",
    SOURCES => [{
        FILE => "Homo_sapiens.gene_info.gz", 
        HOST => "ftp.ncbi.nih.gov",
        REMOTE_DIR => "gene/DATA/GENE_INFO/Mammalia",
        EXTRACT => 1,
    }],
};

1;
