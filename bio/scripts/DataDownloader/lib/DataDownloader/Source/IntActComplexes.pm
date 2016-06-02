package DataDownloader::Source::IntActComplexes;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => "IntAct Complexes",
    DESCRIPTION => "Complex protein interaction data from IntAct",
    SOURCE_LINK => "http://www.ebi.ac.uk/intact",
    HOST        => "ftp.ebi.ac.uk",
    SOURCE_DIR => 'psi/intact/complexes',
};



use constant FILE_TYPES =>
  qr/xml/;

sub BUILD {
    my $self = shift;
        for my $file ($self->ls_remote_dir("pub/databases/IntAct/complex/current/psi25/Homo_sapiens")) {
            $self->add_source(
                HOST       => 'ftp.ebi.ac.uk',
                REMOTE_DIR => "pub/databases/IntAct/complex/current/psi25/Homo_sapiens",
                FILE       => $file,
            ) if ( $file =~ FILE_TYPES );

    }
}

1;

