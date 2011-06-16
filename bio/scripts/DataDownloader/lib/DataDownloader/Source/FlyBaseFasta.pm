package DataDownloader::Source::FlyBaseFasta;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE       => "FlyBase Sequence data",
    DESCRIPTION => "Fasta Files from FlyBase",
    SOURCE_LINK => 'http://flybase.org',
    HOST        => "ftp.flybase.net",
    SOURCE_DIR  => "flybase/fasta",
};

use constant FILE_TYPES =>
  qr/all-CDS|all-gene-|all-five_prime_UTR|all-three_prime_UTR/;

sub BUILD {
    my $self = shift;

    for my $species ( grep {length == 4} $self->ls_remote_dir("genomes") ) {
        for my $file ($self->ls_remote_dir("genomes/$species/current/fasta")) {
            $self->add_source(
                HOST       => 'ftp.flybase.net',
                REMOTE_DIR => "genomes/$species/current/fasta",
                FILE       => $file,
                EXTRACT    => 1,
            ) if ( $file =~ FILE_TYPES );
        };
    }
}

sub generate_version {
    my $self  = shift;
    for my $file ($self->ls_remote_dir("releases/current")) {
        return $file if ( $file =~ /^FB/ );
    }
}

1;
