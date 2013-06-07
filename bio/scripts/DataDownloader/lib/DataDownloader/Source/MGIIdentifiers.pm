package DataDownloader::Source::MGIIdentifiers;

use Moose;
extends 'DataDownloader::Source::FtpBase';

# ftp://ftp.informatics.jax.org/pub/reports/MGI_Coordinate.rpt
# UPDATE: MGI_Coordinate.rpt was renamed to MGI_Coordinate_build37.rpt on 09/01/2013

use constant {
    TITLE => "MGI Identifiers",
    DESCRIPTION => "Identifiers from MGI",
    SOURCE_LINK => "ftp.informatics.jax.org",
    SOURCE_DIR => "mgi-identifiers",
    SOURCES => [{
        FILE => "MRK_List2.rpt", 
        HOST => "ftp.informatics.jax.org",
        REMOTE_DIR => "pub/reports",
    }],
};

1;
