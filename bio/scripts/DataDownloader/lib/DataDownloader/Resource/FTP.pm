package DataDownloader::Resource::FTP;

use Moose;
use MooseX::FollowPBP;
use Number::Format qw(format_bytes);
extends 'DataDownloader::Resource::ABC';
with 'DataDownloader::Role::FTP';

sub fetch {
    my $self = shift;
    my $file = $self->get_file;
    my $host = $self->get_host;
    my $temp = $self->get_temp_file;
    my $con  = $self->get_ftp;

    my $bytes = $con->size($file);
    my $size = format_bytes($bytes);

    $self->debug("Downloading $file ($size) from $host to $temp");
    # Show progress hashes for files larger then 1,000,000 bytes
    if ($bytes > 1_000_000) {
        $con->hash(\*STDERR, ($bytes / 80));
    }

    $con->get("$file", "$temp") or die "Failed to download $file - ", $con->message;

    my $dl_size = -s $temp;
    warn "Downloaded file is wrong size ($dl_size != $bytes)" unless ($dl_size == $bytes);

    $self->make_destination(
        $self->get_temp_file => $self->get_destination
    );
    $self->clean_up();
}

after fetch => sub {
    my $self = shift;
    $self->get_ftp->quit();
};

1;
