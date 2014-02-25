package DataDownloader::Source::GenBank;

use Moose;
extends 'DataDownloader::Source::FtpBase';
use MooseX::FollowPBP;
use Ouch qw(:traditional);

## TODO
## 1. Download all files in the remote directory
## 2. Read synbiomine.yml to parse Genbank config and set REMOTE_DIR and SUB_DIR 

use constant {
    TITLE => 'GenBank',
    DESCRIPTION => 'An annotated collection of all publicly available DNA sequences',
    SOURCE_LINK => 'http://www.ncbi.nlm.nih.gov/genbank/',
    SOURCE_DIR => 'genbank', 

    SOURCES => [
        {
            HOST => 'ftp.ncbi.nlm.nih.gov',
            REMOTE_DIR => 'genomes/Bacteria/Bacillus_subtilis_168_uid57675',
            FILE       => "NC_000964.gff",
            SUB_DIR    => ["Bacillus_subtilis_168"],
        },

        {
            HOST => 'ftp.ncbi.nlm.nih.gov',
            REMOTE_DIR => 'genomes/Bacteria/Escherichia_coli_K_12_substr__MG1655_uid57779',
            FILE       => "NC_000913.gff",
            SUB_DIR    => ["Escherichia_coli_K_12_substr__MG1655"],
        },

        {
            HOST => 'ftp.ncbi.nlm.nih.gov',
            REMOTE_DIR => 'genomes/Bacteria/Geobacillus_thermoglucosidasius_C56_YS93_uid48129',
            FILE       => "NC_015660.gff",
            SUB_DIR    => ["Geobacillus_thermoglucosidasius_C56_YS93"],
        },
    ],
};

1;
