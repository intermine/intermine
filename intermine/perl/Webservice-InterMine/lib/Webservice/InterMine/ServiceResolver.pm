package Webservice::InterMine::ServiceResolver;

use Moose;
with 'Webservice::InterMine::Role::KnowsJSON';
use LWP::UserAgent;
use Webservice::InterMine::Types qw/UserAgent/;

use constant REGISTRY => 'http://www.intermine.org/registry/mines.json';

has ua => (
    is => 'ro', 
    isa => UserAgent,
    default => sub {LWP::UserAgent->new()},
);

my $data;
my $fetches = 0;

sub get_fetch_count {
    return $fetches;
}

sub resolve {
    my $self = shift;
    my $service_name = shift;
    unless ($data) {
        my $response = $self->ua->get(REGISTRY);
        confess $response->status_line if $response->is_error;
        $data = eval {$self->decode($response->content);}
            or confess "Error decoding " . $response->content;
        $fetches++;
    }
    for my $mine (@{$data->{mines}}) {
        if (lc($mine->{name}) eq lc($service_name)) {
            return $mine->{webServiceRoot};
        }
    }
    confess "Could not resolve $service_name";
}

no Moose;
__PACKAGE__->meta->make_immutable;

1;
