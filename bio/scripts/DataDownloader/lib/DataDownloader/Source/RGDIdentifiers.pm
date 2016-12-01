package DataDownloader::Source::RGDIdentifiers;

use Moose;
extends 'DataDownloader::Source::FtpBase';

# ftp://rgd.mcw.edu/pub/data_release/GENES_RAT.txt

use constant {
    TITLE => "RGD Identifiers",
    DESCRIPTION => "Identifiers from MCW",
    SOURCE_LINK => "ftp://rgd.mcw.edu",
    SOURCE_DIR => "rgd-identifiers",
    SOURCES => [{
        FILE => "GENES_RAT.txt", 
        HOST => "ftp.rgd.mcw.edu",
        REMOTE_DIR => "pub/data_release",
    }],
};

1;


