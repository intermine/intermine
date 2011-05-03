package DataDownloader::Source::FtpBase;

use Moose;
use MooseX::ABC;
use MooseX::FollowPBP;
use Net::FTP;
extends 'DataDownloader::Source::ABC';
with 'DataDownloader::Role::FTP';

use Ouch qw(:traditional);
use DataDownloader::Util 'get_ymd';

use constant METHOD => 'FTP';

has reference_file => (
    is => 'ro',
    isa => 'Maybe[Str]',
    lazy_build => 1,
);

sub _build_reference_file  {
    my $self = shift;
    if ($self->can('REF_FILE')) {
        return $self->REF_FILE;
    }
}

override generate_version => sub {
    my $self = shift;
    if ($self->get_reference_file) {
        my $ftp = $self->get_ftp;
        if ($ftp->feature('MDTM')) {
            my $mod_time = $ftp->mdtm($self->get_reference_file);
            return get_ymd($mod_time);
        } else {
            throw "Download Error", "Reference file provided for versioning - but this server does not support MDTM";
        }
    }
    return super;
};

1;
