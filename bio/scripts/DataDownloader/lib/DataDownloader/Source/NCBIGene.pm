package DataDownloader::Source::NCBIGene;

use Moose;
extends 'DataDownloader::Source::FtpBase';

# human - ftp://ftp.ncbi.nih.gov/gene/DATA/GENE_INFO/Mammalia/Homo_sapiens.gene_info.gz
# all - ftp://ftp.ncbi.nih.gov/gene/DATA/GENE_INFO/All_Data.gene_info.gz

use constant {
    TITLE => "NCBI Gene ",
    DESCRIPTION => "Genes from NCBI",
    SOURCE_LINK => "ftp.ncbi.nih.gov",
    SOURCE_DIR => "ncbi",
    SOURCES => [{
        FILE => "All_Data.gene_info.gz", 
        HOST => "ftp.ncbi.nih.gov",
        REMOTE_DIR => "gene/DATA/GENE_INFO",
        EXTRACT => 1,
    }],
};

1;
