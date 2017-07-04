package DataDownloader::Source::MGIIdentifiers;

use Moose;
extends 'DataDownloader::Source::ABC';

# ftp://ftp.informatics.jax.org/pub/reports/MGI_Coordinate.rpt
# UPDATE: MGI_Coordinate.rpt was renamed to MGI_Coordinate_build37.rpt on 09/01/2013
# removed FTP server? April 2017

use constant {
    TITLE => "MGI Identifiers",
    DESCRIPTION => "Identifiers from MGI",
    SOURCE_LINK => "www.informatics.jax.org",
    SOURCE_DIR => "mgi-identifiers",
    SOURCES => [{
        FILE => "MRK_List2.rpt", 
        URI => "http://www.informatics.jax.org/downloads/reports",
    }],
};

1;
