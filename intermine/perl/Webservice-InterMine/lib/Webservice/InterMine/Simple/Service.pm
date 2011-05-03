package Webservice::InterMine::Simple::Service;

use strict;
use Webservice::InterMine::Simple::Query;
use LWP;
use MIME::Base64;

use constant USER_AGENT => 'WebserviceInterMinePerlAPIClient';

sub new {
    my $class = shift;
    my $self = {@_};
    my $ua = LWP::UserAgent->new;
    $ua->env_proxy;
    $ua->agent(USER_AGENT);
    if ($self->{user} and $self->{pass}) {
        my $auth_string = join(':', $self->{user}, $self->{pass});
        $ua->default_header( Authorization => encode_base64($auth_string) );
    }
    $self->{ua} = $ua;
    return bless $self, $class;
}

sub new_from_xml {
    my $self = shift;
    my %args = @_;
    $args{service} = $self;
    return Webservice::InterMine::Simple::Query->new_from_xml(%args);
}

sub new_query {
    my $self = shift;
    my %args = @_;
    $args{service} = $self;
    return Webservice::InterMine::Simple::Query->new(%args);
}

sub template {
    my $self = shift;
    my $name = shift;
    my %args = (
        name => $name,
        service => $self,
    );
    return Webservice::InterMine::Simple::Template->new(%args);
}

1;

