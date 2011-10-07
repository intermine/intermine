package DataDownloader::Source::InterPro;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => 'InterPro protein family and domain data',
    DESCRIPTION => "Protein Family and Domain data from Interpro",
    SOURCE_LINK => 'http://www.ebi.ac.uk/interpro',
    SOURCE_DIR  => 'interpro',
    HOST        => "ftp.ebi.ac.uk",
    REMOTE_DIR  => "pub/databases/interpro",
    REF_FILE    => 'interpro.xml.gz',
    EXTRACT => 1,
};


