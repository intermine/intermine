package DataDownloader::Source::OrthoDB;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => 'OrthoDB',
    DESCRIPTION => 'OrthoDB Homologues',
    SOURCE_LINK => 'http://cegg.unige.ch/orthodb',
    SOURCE_DIR => 'orthodb',
    SOURCES => [
        {
            HOST => 'cegg.unige.ch',
            REMOTE_DIR => '/OrthoDB7/',
            FILE => 'OrthoDB7_ALL_METAZOA_tabtext.gz',
            EXTRACT => 1,
        },
        {
            HOST => 'cegg.unige.ch',
            REMOTE_DIR => '/OrthoDB7/',
            FILE => 'OrthoDB7_ALL_FUNGI_tabtext.gz',
            EXTRACT => 1,
        },
        {
            HOST => 'cegg.unige.ch',
            REMOTE_DIR => '/OrthoDB7/',
            FILE => 'OrthoDB7_Firmicutes-Bacilli_tabtext.gz',
            EXTRACT => 1,
        },
        {
            HOST => 'cegg.unige.ch',
            REMOTE_DIR => '/OrthoDB7/',
            FILE => 'OrthoDB7_Gammaproteobacteria_tabtext.gz',
            EXTRACT => 1,
        },

        {
            HOST => 'cegg.unige.ch',
            REMOTE_DIR => '/OrthoDB7/',
            FILE => 'OrthoDB7_Actinobacteria_tabtext.gz',
            EXTRACT => 1,
        },
        {
            HOST => 'cegg.unige.ch',
            REMOTE_DIR => '/OrthoDB7/',
            FILE => 'OrthoDB7_Alphaproteobacteria_tabtext.gz',
            EXTRACT => 1,
        },
        {
            HOST => 'cegg.unige.ch',
            REMOTE_DIR => '/OrthoDB7/',
            FILE => 'OrthoDB7_Betaproteobacteria_tabtext.gz',
            EXTRACT => 1,
        },
        {
            HOST => 'cegg.unige.ch',
            REMOTE_DIR => '/OrthoDB7/',
            FILE => 'OrthoDB7_Cyanobacteria-Chroococcales_tabtext.gz',
            EXTRACT => 1,
        },
        {
            HOST => 'cegg.unige.ch',
            REMOTE_DIR => '/OrthoDB7/',
            FILE => 'OrthoDB7_Cyanobacteria-Prochlorales_tabtext.gz',
            EXTRACT => 1,
        },
        {
            HOST => 'cegg.unige.ch',
            REMOTE_DIR => '/OrthoDB7/',
            FILE => 'OrthoDB7_Firmicutes-Clostridia_tabtext.gz',
            EXTRACT => 1,
        },
        {
            HOST => 'cegg.unige.ch',
            REMOTE_DIR => '/OrthoDB7/',
            FILE => 'OrthoDB7_Spirochaetes-Spirochaetia_tabtext.gz',
            EXTRACT => 1,
        },
        {
            HOST => 'cegg.unige.ch',
            REMOTE_DIR => '/OrthoDB7/',
            FILE => 'OrthoDB7_Tenericutes-Mollicutes_tabtext.gz',
            EXTRACT => 1,
        },
        {
            HOST => 'cegg.unige.ch',
            REMOTE_DIR => '/OrthoDB7/',
            FILE => 'OrthoDB7_Thermotogae_tabtext.gz',
            EXTRACT => 1,
        },
    ],
};

1;
