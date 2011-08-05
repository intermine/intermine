package DataDownloader::Resource::FTP;

use Moose;
use Ouch qw/:traditional/;
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

    my $was_successful = $con->get("$file", "$temp");
    my $dl_size = -s $temp;
    unless ($was_successful or ($dl_size == $bytes)) {
        throw "Download Error" => $con->message;
    }

    if ($bytes and $dl_size != $bytes) {
        throw "Download Error" => "$file is wrong size ($dl_size != $bytes)";
    }
    unless ($dl_size) {
        $self->debug("Not creating " . $self->get_destination . " as the downloaded file is empty");
        return;
    }

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
