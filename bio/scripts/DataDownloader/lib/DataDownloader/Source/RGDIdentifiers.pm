package DataDownloader::Source::RGDIdentifiers;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => "RGD Identifiers",
    DESCRIPTION => "Identifiers from MCW",
    SOURCE_LINK => "http://rgd.mcw.edu",
    HOST => "rgd.mcw.edu",
    SOURCE_DIR => "rgd-identifiers",
    REMOTE_DIR => "pub/data_release",
    FILE => "GENES_RAT.txt", 
};

1;
