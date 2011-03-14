package Webservice::InterMine::Query::Roles::QueryUrl;

use Moose::Role;

requires qw(service QUERY_PATH Get_request_parameters);

sub url {
    my $self = shift;
    my %args = @_;
    my %query_form = $self->get_request_parameters;
    $query_form{format} = $args{format} || 'tab';
    
    # Set optional parameters
    for my $opt (qw/start size addheader/) {
        $query_form{$opt} = $args{$opt} if ($args{$opt});
    }

    my $url = $self->service->root . $self->QUERY_PATH;
    my $uri = URI->new($url);
    $uri->query_form(%query_form);
    return $uri;
}

1;
