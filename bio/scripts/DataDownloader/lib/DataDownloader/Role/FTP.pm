package DataDownloader::Role::FTP;

use Moose::Role;
use MooseX::FollowPBP;
use Net::FTP;

has host => (
    init_arg => 'HOST',
    isa => 'Str',
    is => 'ro',
    required => 1,
    lazy_build => 1,
);

sub _build_host  {
    my $self = shift;
    if ($self->can("HOST")) {
        return $self->HOST;
    }
}

has remote_dir => (
    init_arg => 'REMOTE_DIR',
    isa => 'Str', 
    is => 'ro',
    required => 1,
    lazy_build => 1,
);

sub _build_remote_dir  {
    my $self = shift;
    if ($self->can("REMOTE_DIR")) {
        return $self->REMOTE_DIR;
    }
}

has login_credentials => (
    init_arg => 'CREDENTIALS',
    isa => 'ArrayRef',
    is => 'ro',
    default => sub { [] },
    auto_deref => 1,
);

has ftp => (
    isa => 'Net::FTP',
    is => 'ro', 
    builder => 'connect',
    lazy_build => 1,
);

=head2 connect

Return a connected, logged in ftp handle in the remote directory
specified by get_remote_dir.

=cut

sub connect {
    my $self = shift;
    my $dir = shift || $self->get_remote_dir;

    my $host = $self->get_host;
    my $ftp = Net::FTP->new($host, Passive => 1)
        or $self->die("Cannot connect to $host: $@\n");
    my @credentials = $self->get_login_credentials;
    $ftp->login(@credentials)
        or $self->die("Cannot login to $host - " . $ftp->message);
    $ftp->cwd($dir) or $self->die("Cannot change working directory to $dir - " . $ftp->message);
    $ftp->binary or $self->die("Cannot set binary mode: $!");
    return $ftp;
}

sub ls_remote_dir {
    my $self = shift;
    my $ftp = $self->connect(@_);
    my @things = $ftp->ls;
    $ftp->quit;
    return @things;
}

1;
