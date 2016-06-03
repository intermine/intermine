package DataDownloader::Source::NCBIGeneHuman;

use Moose;
extends 'DataDownloader::Source::FtpBase';

# human - ftp://ftp.ncbi.nih.gov/gene/DATA/GENE_INFO/Mammalia/Homo_sapiens.gene_info.gz
# used to get gene summaries

use constant {
    TITLE => "NCBI Gene - Human",
    DESCRIPTION => "Human Genes from NCBI",
    SOURCE_LINK => "ftp.ncbi.nih.gov",
    SOURCE_DIR => "ncbi/gene-info-human",
    SOURCES => [{
        FILE => "Homo_sapiens.gene_info.gz", 
        HOST => "ftp.ncbi.nih.gov",
        REMOTE_DIR => "gene/DATA/GENE_INFO/Mammalia",
        EXTRACT => 1,
    }],
};

1;
