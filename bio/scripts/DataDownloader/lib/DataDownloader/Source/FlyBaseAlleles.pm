package DataDownloader::Source::FlyBaseAlleles;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE => 'FlyBase Alleles',
    DESCRIPTION => 'Alleles from FlyBase',
    SOURCE_LINK => 'http://flybase.org',
    SOURCE_DIR => 'flybase/alleles',
    HOST => "ftp.flybase.net",
};

use constant FILE_TYPES =>
  qr/tsv.gz/;

sub BUILD {
    my $self = shift;
        for my $file ($self->ls_remote_dir("releases/current/precomputed_files/human_disease")) {
            $self->add_source(
                HOST       => 'ftp.flybase.net',
                REMOTE_DIR => "releases/current/precomputed_files/human_disease",
                FILE       => $file,
                EXTRACT    => 1,
            ) if ( $file =~ FILE_TYPES );
    }
}

1;
