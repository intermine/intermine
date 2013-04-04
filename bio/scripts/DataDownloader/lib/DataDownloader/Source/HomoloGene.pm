package DataDownloader::Source::HomoloGene;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => 'HomoloGene',
    DESCRIPTION => 'HomoloGene Homologues',
    SOURCE_LINK => 'http://www.ncbi.nlm.nih.gov/homologene',
    SOURCE_DIR => 'homologene',
    SOURCES => [
        {
            HOST => 'ftp.ncbi.nih.gov',
            REMOTE_DIR => '/pub/HomoloGene/current/',
            FILE => 'homologene.data',
            EXTRACT => 0,
        },
    ],
};

1;
