package DataDownloader::Source::FlyBaseAlleles;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => 'FlyBase Alleles',
    DESCRIPTION => 'Alleles from FlyBase',
    SOURCE_LINK => 'http://flybase.org',
    SOURCE_DIR => 'flybase/alleles',
    SOURCES => [
        {
            SUBTITLE => 'Human Disease',
            HOST => 'ftp.flybase.net',
            REMOTE_DIR => "releases/current/precomputed_files/human_disease",
            FILE => 'allele_human_disease_model_data_fb_2017_02.tsv.gz',
            EXTRACT => 1,
        },
    ],
};

1;
