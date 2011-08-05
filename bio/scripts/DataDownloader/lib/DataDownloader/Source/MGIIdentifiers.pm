package DataDownloader::Source::MGIIdentifiers;

use Moose;
extends 'DataDownloader::Source::FtpBase';

# ftp://ftp.informatics.jax.org/pub/reports/MGI_Coordinate.rpt

use constant {
    TITLE => "MGI Identifiers",
    DESCRIPTION => "Identifiers from MGI",
    SOURCE_LINK => "ftp.informatics.jax.org",
    SOURCE_DIR => "mgi-identifiers",
    SOURCES => [{
        FILE => "MGI_Coordinate.rpt", 
        HOST => "ftp.informatics.jax.org",
        REMOTE_DIR => "pub/reports",
    }],
};

1;
