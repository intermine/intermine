package DataDownloader::Source::RGDIdentifiers;

use Moose;
extends 'DataDownloader::Source::FtpBase';

# ftp://rgd.mcw.edu/pub/data_release/GENES_RAT.txt

use constant {
    TITLE => "RGD Identifiers",
    DESCRIPTION => "Identifiers from MCW",
    SOURCE_LINK => "ftp://rgd.mcw.edu",
    HOST => "ftp://rgd.mcw.edu",
    SOURCE_DIR => "rgd-identifiers",
    REMOTE_DIR => "pub/data_release",
    FILE => "GENES_RAT.txt", 
};

1;
