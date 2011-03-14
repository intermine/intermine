package Webservice::InterMine::Query::Roles::QueryParameters;

use Moose::Role;

requires qw(to_xml to_legacy_xml service);

sub resource_path {
    my $self = shift;
    return $self->service->QUERY_PATH;
}

sub get_request_parameters {
    my $self = shift;
    
    my $xml = ( $self->service->version < 2 ) 
        ? $self->to_legacy_xml 
        : $self->to_query_xml;

   return (query => $xml);
}

1;
    

