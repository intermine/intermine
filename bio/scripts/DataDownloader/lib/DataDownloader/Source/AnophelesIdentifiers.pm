package DataDownloader::Source::AnophelesIdentifiers;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use XML::Parser::PerlSAX;
use PerlIO::gzip;
use Number::Format qw(format_bytes);

use autodie qw(close open);

# ftp://ftp.vectorbase.org/public_data/organism_data/agambiae/Other/AgamP3.3-to-AgamP3.4-Identifiers.tgz


use constant {
    TITLE => 'Anopheles Identifiers',
    DESCRIPTION =>
"Gene Stable ID mappings from geneset AgamP3.3 to AgamP3.4",
    SOURCE_LINK => 'http://agambiae.vectorbase.org/GetData/Downloads/',
    SOURCE_DIR  => 'anopheles-identifiers',
    HOST        => "ftp.vectorbase.org",
    REMOTE_DIR  => "public_data/organism_data/agambiae/Other",
    REF_FILE    => 'AgamP3.3-to-AgamP3.4-Identifiers.tgz',
};

