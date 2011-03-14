package Webservice::InterMine::Simple::Query;

use strict;
use URI;

use constant 'RESOURCE_PATH' => '/template/results';

sub new {
    my $class = shift;
    my $self = {@_};
    return bless $self, $class;
}

sub get_uri {
    my $self = shift;
    my $uri = URI->new($self->{service}{root} . RESOURCE_PATH);
    return $uri;
}

sub result_with {
    my $self = shift;
    my %args = @_;
    my $uri = $self->get_uri;
    my $format = delete $args{as};
    my %query_form = (name => $self->{name}, format => $format, %args);
    $uri->query_form(%query_form);
    my $result = $self->{service}{ua}->get($uri);
    if ($result->is_success) {
        return $result->content;
    } else {
        die $result->status_line, $result->content;
    }
}

1;
    
        
