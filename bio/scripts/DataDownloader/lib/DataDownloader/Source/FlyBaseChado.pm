package DataDownloader::Source::FlyBaseChado;

use Moose;
extends 'DataDownloader::Source::FtpBase';

use constant {
    TITLE       => "FlyBase genomic data",
    DESCRIPTION => "Chado dumps from FlyBase",
    SOURCE_LINK => 'http://flybase.org',
    HOST        => "ftp.flybase.net",
    SOURCE_DIR  => "flybase/chado",
    FILE_TYPES => qr/sql/,
};

sub BUILD {
    my $self = shift;
    my $current = $self->get_version;
    $self->debug("Current FlyBase version is $current");
    for my $file ($self->ls_remote_dir("releases/$current/psql")) {
        $self->add_source(
            HOST       => 'ftp.flybase.net',
            REMOTE_DIR => "releases/$current/psql",
            FILE       => $file,
        ) if ( $file =~ FILE_TYPES );
    };
}

sub generate_version {
    my $self  = shift;
    for my $file ($self->ls_remote_dir("releases/current")) {
        return $file if ( $file =~ /^FB/ );
    }
}

1;
