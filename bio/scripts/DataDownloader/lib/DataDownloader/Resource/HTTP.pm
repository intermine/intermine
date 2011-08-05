package DataDownloader::Resource::HTTP;

use Moose;
extends 'DataDownloader::Resource::ABC';

use MooseX::FollowPBP;
require LWP::UserAgent;

use Ouch qw(:traditional);

sub BUILD {
    my $self = shift;
    unless ($self->has_server or $self->has_uri) {
        throw DownloadError => "Must provide a server or a URI for each resource";
    }
}

has server => (
    init_arg => 'SERVER',
    isa => 'Str',
    is => 'ro',
    predicate => 'has_server',
);

has uri => (
    init_arg => 'URI', 
    isa => 'Str',
    is => 'ro',
    predicate => 'has_uri',
);

has user_agent => (
    is => 'ro',
    isa => 'LWP::UserAgent',
    lazy_build => 1,
    builder => 'build_user_agent',
);

has header_checker => (
    init_arg => 'HEADER_CHECKER',
    traits => ['Code'],
    handles => { check_headers => 'execute_method' },
    isa => 'CodeRef',
    is => 'ro',
    predicate => 'has_header_checker',
    default => sub {\&default_error_checker},
);

sub default_error_checker {
    my $self = shift;
    my $response = shift or confess "No response";
    if ($response->filename && $response->filename =~ /(error|nosuchfile|filenotfound|404)/i) {
        # Some services (PDB for example) provide 200 ok and some HTML for missing files
        return;
    } elsif ($response->content) {
        return 1;
    } else {
        $self->debug("Successful request, but No content received - skipping");
        return;
    }
}

sub build_user_agent {
    return LWP::UserAgent->new;
}

sub fetch {
    my $self = shift;
    my $source = ($self->has_uri) 
        ? $self->get_uri
        : $self->get_server . '/' . $self->get_file;
    $self->debug("Downloading $source to " . $self->get_temp_file);

    my $response = $self->get_user_agent->get($source);
    if ($response->is_error()) {
        $self->die($response->status_line());
    } else {
        if ($self->check_headers($response)) {
            my $fh = $self->get_temp_file->openw();
            $fh->print($response->content);
        }
    }

    unless (-s $self->get_temp_file) {
        $self->debug("Not creating " . $self->get_destination . " as the downloaded file is empty");
        return;
    }

    $self->make_destination(
        $self->get_temp_file => $self->get_destination);
    $self->clean_up();
}

1;
